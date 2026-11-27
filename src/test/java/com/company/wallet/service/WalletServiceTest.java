package com.company.wallet.service;

import com.company.wallet.domain.*;
import com.company.wallet.exception.*;
import com.company.wallet.repository.InMemoryTransactionRepository;
import com.company.wallet.repository.InMemoryWalletRepository;
import com.company.wallet.repository.TransactionRepository;
import com.company.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WalletServiceTest {
    private static final String VND = Currency.getInstance("VND").toString();
    private static final String USD = Currency.getInstance("USD").toString();
    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private WalletService walletService;
    private Wallet sourceWallet;
    private Wallet destinationWallet;
    @BeforeEach
    void setUp() {
        walletRepository = new InMemoryWalletRepository();
        transactionRepository = new InMemoryTransactionRepository();
        walletService = new WalletService(walletRepository, transactionRepository);
        sourceWallet = walletService.createWallet("CUSTOMER001", VND);
        destinationWallet = walletService.createWallet("CUSTOMER002", VND);
    }
    //1. Tạo ví thành công (kiểm tra walletId không null, số dư khởi tạo bằng 0, trạng thái ACTIVE).
    @Test
    @DisplayName("01. Tạo ví thành công")
    void createWallet_shouldInitializeWalletCorrectly() {
        Wallet wallet = walletService.createWallet("CUSTOMER003", VND);
        assertNotNull(wallet.getWalletId());
        assertMoneyEquals("0", walletService.getBalance(wallet.getWalletId()));
        assertEquals("ACTIVE", wallet.getStatus().name());
    }
    //2. Nạp tiền hợp lệ (số dư tăng đúng, lưu Transaction trạng thái SUCCESS).
    @Test
    @DisplayName("02. Nạp tiền hợp lệ")
    void depositWallet_shouldIncreaseBalanceAndSaveSuccessfullyTransaction() {
        walletService.deposit(sourceWallet.getWalletId(), money("100000",VND));
        assertMoneyEquals("100000", walletService.getBalance(sourceWallet.getWalletId()));
        List<Transaction> transactions = walletService.getTransactionHistory(sourceWallet.getWalletId());
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
        assertEquals(TransactionStatus.SUCCESS, transactions.get(0).getStatus());
    }

    //3. Nạp tiền âm (ném IllegalArgumentException khi khởi tạo Money hoặc InvalidAmountException).
    @Test
    @DisplayName("03. Nạp tiền âm")
    void deposit_shouldRejectNegativeAmount() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Money negativeAmount = new Money(-10000, VND);
            walletService.deposit(sourceWallet.getWalletId(), negativeAmount);
        });
        assertTrue(exception instanceof IllegalArgumentException || exception instanceof InvalidAmountException, "Phải từ chối số tiền âm");
        assertMoneyEquals("0", walletService.getBalance(sourceWallet.getWalletId()));
    }

    //4. Nạp tiền bằng 0 (ném InvalidAmountException, lưu Transaction trạng thái FAILED).
    @Test
    @DisplayName("04. Nạp tiền bằng 0")
    void deposit_ShouldRejectZeroAmountAndSaveFailedTransaction() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Money zeroAmount = new Money(0, VND);
            walletService.deposit(sourceWallet.getWalletId(), zeroAmount);
        });
        assertTrue(exception instanceof InvalidAmountException,"Phải từ chối số tiền ầm");
        assertMoneyEquals("0", walletService.getBalance(sourceWallet.getWalletId()));
        assertLatestTransactionFailed(sourceWallet.getWalletId(), TransactionType.DEPOSIT);
    }

    //5. Rút tiền hợp lệ (số dư giảm đúng, lưu Transaction SUCCESS).
    @Test
    @DisplayName("05. Rút tiền hợp lệ")
    void withdrawWallet_shouldDecreaseBalanceAndSaveSuccessfullyTransaction() {
        walletService.deposit(sourceWallet.getWalletId(), money("100000",VND));
        walletService.withdraw(sourceWallet.getWalletId(), money("50000",VND));
        assertMoneyEquals("50000", walletService.getBalance(sourceWallet.getWalletId()));
        List<Transaction> transactions = walletService.getTransactionHistory(sourceWallet.getWalletId());
        Transaction withdrawal = transactions.get(transactions.size() - 1);
        assertEquals(TransactionType.WITHDRAW, withdrawal.getType());
        assertEquals(TransactionStatus.SUCCESS, withdrawal.getStatus());
    }
    //6. Rút vượt số dư (ném InsufficientBalanceException, số dư giữ nguyên, lưu Transaction FAILED).
    @Test
    @DisplayName("06. Rút vượt số dư")
    void withdraw_shouldRejectAmountGreaterThanBalanceAndSaveFailedTransaction() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.deposit(sourceWallet.getWalletId(), money("100000",VND));
            walletService.withdraw(sourceWallet.getWalletId(), money("500000",VND));
        });
        assertTrue(exception instanceof InsufficientBalanceException, "Phải từ chối số tiền rút vượt quá số dư");
        assertMoneyEquals("100000", walletService.getBalance(sourceWallet.getWalletId()));
        assertLatestTransactionFailed(sourceWallet.getWalletId(), TransactionType.WITHDRAW);
    }
    //7. Rút tiền âm hoặc bằng 0.
    //7a. Rút tiền âm
    @Test
    @DisplayName("07a. Rút tiền ầm")
    void withdraw_shouldRejectNegativeAmountAndSaveFailedTransaction() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.withdraw(sourceWallet.getWalletId(), money("-100000",VND));
        });
        assertTrue(exception instanceof IllegalArgumentException || exception instanceof InvalidAmountException, "Phải từ chối số tiền âm");
        assertMoneyEquals("0", walletService.getBalance(sourceWallet.getWalletId()));
    }
    //7b. Rút tiền bằng 0
    @Test
    @DisplayName("07b. Rút tiền bằng 0")
    void withdraw_shouldRejectZeroAmountAndSaveFailedTransaction() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            walletService.withdraw(sourceWallet.getWalletId(), money("0",VND));
        });
        assertTrue(exception instanceof IllegalArgumentException || exception instanceof InvalidAmountException, "Phải từ chối số tiền âm");
        assertMoneyEquals("0", walletService.getBalance(sourceWallet.getWalletId()));
    }
    //8. Chuyển tiền hợp lệ (trừ ví nguồn, cộng ví đích, cả 2 ví đều thấy giao dịch TRANSFER SUCCESS trong getTransactionHistory).
    @Test
    @DisplayName("08. Chuyển tiền hợp lệ")
    void transfer_shouldMoveMoneyAndAppearInBothWalletHistories(){
        walletService.deposit(sourceWallet.getWalletId(), money("100000",VND));
        walletService.transfer(sourceWallet.getWalletId(), destinationWallet.getWalletId(), money("20000",VND));
        assertMoneyEquals("80000", walletService.getBalance(sourceWallet.getWalletId()));
        assertMoneyEquals("20000", walletService.getBalance(destinationWallet.getWalletId()));
        Transaction sourceWalletHistoryLast = last(walletService.getTransactionHistory(sourceWallet.getWalletId()));
        Transaction destWalletHistoryLast = last(walletService.getTransactionHistory(destinationWallet.getWalletId()));
        assertEquals(TransactionType.TRANSFER, sourceWalletHistoryLast.getType());
        assertEquals(TransactionStatus.SUCCESS, sourceWalletHistoryLast.getStatus());
        assertEquals(sourceWalletHistoryLast.getTransactionId(), destWalletHistoryLast.getTransactionId());
    }
    //9. Chuyển vào chính ví nguồn (ném SelfTransferException, số dư không đổi).
    @Test
    @DisplayName("09.Không cho phép chuyển tiền vào chính ví nguồn")
    void transfer_shouldRejectSelfTransferWithoutChangingBalance(){
        walletService.deposit(sourceWallet.getWalletId(), money("100000", VND));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
           walletService.transfer(sourceWallet.getWalletId(), sourceWallet.getWalletId(), money("20000", VND));
        });
        assertTrue(exception instanceof SelfTransferException, "Phải từ chối chuyển vào chính ví nguồn");
        assertMoneyEquals("100000", walletService.getBalance(sourceWallet.getWalletId()));
    }
    //10. Chuyển tiền vượt số dư (ném InsufficientBalanceException, số dư cả 2 ví giữ nguyên).
    @Test
    @DisplayName("10. Chuyển tiền vượt số dư")
    void transfer_shouldRejectInsufficientBalanceWithoutChangingEitherWallet() {
        walletService.deposit(sourceWallet.getWalletId(), money("50000", VND));
        assertThrows(InsufficientBalanceException.class,
                () -> walletService.transfer(sourceWallet.getWalletId(), destinationWallet.getWalletId(), money("80000", VND))
        );
        assertMoneyEquals("50000", walletService.getBalance(sourceWallet.getWalletId()));
        assertMoneyEquals("0", walletService.getBalance(destinationWallet.getWalletId()));
    }
    //11. Chuyển tiền bằng 0 (ném InvalidAmountException, số dư cả 2 ví giữ nguyên).
    @Test
    @DisplayName("11. Chuyển tiền bằng 0")
    void transfer_shouldRejectZeroAmountWithoutChangingEitherWallet() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.transfer(sourceWallet.getWalletId(), destinationWallet.getWalletId(), money("0", VND))
        );
        assertMoneyEquals("0", walletService.getBalance(sourceWallet.getWalletId()));
        assertMoneyEquals("0", walletService.getBalance(destinationWallet.getWalletId()));
    }
    //12. Chuyển tiền thất bại (ví dụ: ví đích đang bị freeze()) không làm thay đổi số dư ví nguồn và ví đích.
    @Test
    @DisplayName("12. Chuyển tiền vào ví đích đang bị khóa")
    void transfer_shouldNotChangeBalancesWhenDestinationWalletIsFrozen(){
        walletService.deposit(sourceWallet.getWalletId(), money("100000", VND));
        destinationWallet.freeze();
        assertThrows(WalletInactiveException.class,
                () -> walletService.transfer(sourceWallet.getWalletId(), destinationWallet.getWalletId(), money("20000", VND))
                );
        assertMoneyEquals("100000", walletService.getBalance(sourceWallet.getWalletId()));
        assertMoneyEquals("0", walletService.getBalance(destinationWallet.getWalletId()));
    }
    //13. Thao tác trên ví không tồn tại (WalletNotFoundException).
    @Test
    @DisplayName("13. Thao tác trên ví không tồn tại")
    void deposit_shouldThrowWhenWalletDoesNotExist() {
        assertThrows(WalletNotFoundException.class,
                () -> walletService.deposit("UNKNOWN_WALLET", money("10000", VND))
        );
    }
    //14. Nạp/Rút tiền trên ví đang bị khóa (freeze() -> WalletInactiveException).
    //14a. Nạp tiền vào ví đang bị khóa
    @Test
    @DisplayName("14a. Nạp tiền vào ví đang bị khóa")
    void deposit_shouldRejectFrozenWallet() {
        sourceWallet.freeze();
        assertThrows(WalletInactiveException.class,
                () -> walletService.deposit(sourceWallet.getWalletId(), money("10000", VND))
        );
        assertMoneyEquals("0", walletService.getBalance(sourceWallet.getWalletId()));
    }
    //14b. Rút tiền vào ví đang bị khóa
    @Test
    @DisplayName("14b. Rút tiền từ ví đang bị khóa")
    void withdraw_shouldRejectFrozenWallet() {
        walletService.deposit(sourceWallet.getWalletId(), money("50000", VND));
        sourceWallet.freeze();
        assertThrows(WalletInactiveException.class,
                () -> walletService.withdraw(sourceWallet.getWalletId(), money("10000", VND))
        );
        assertMoneyEquals("50000", walletService.getBalance(sourceWallet.getWalletId()));
    }
    //15. Chuyển tiền khác đơn vị tiền tệ (ví VND chuyển sang ví USD -> ném lỗi, số dư cả 2 ví không đổi).
    @Test
    @DisplayName("15. Chuyển tiền khác đơn vị tiền tệ")
    void transfer_shouldRejectDifferentCurrenciesWithoutChangingBalances(){
        walletService.deposit(sourceWallet.getWalletId(), money("100000", VND));
        Wallet usdWallet = walletService.createWallet("CUSTOMER_USD", USD);
        assertThrows(IllegalArgumentException.class,
                () -> walletService.transfer(sourceWallet.getWalletId(), usdWallet.getWalletId(), money("50000", VND))
        );
        assertMoneyEquals("100000", walletService.getBalance(sourceWallet.getWalletId()));
        assertMoneyEquals("0", walletService.getBalance(usdWallet.getWalletId()));
    }
    private static Money money(String amount, String currency) {
        return new Money(new BigDecimal(amount).setScale(2,RoundingMode.HALF_UP), currency);
    }
    private static void assertMoneyEquals(String expected, Money actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.getAmount()), "Số tiền thực tế không đúng");
    }
    private static Transaction last(List<Transaction> transactions) {
        assertFalse(transactions.isEmpty(), "Lịch sử giao dịch không được rỗng");
        return transactions.get(transactions.size() - 1);
    }
    private void assertLatestTransactionFailed(String walletId, TransactionType expectedType) {
        Transaction transaction = last(walletService.getTransactionHistory(walletId));
        assertEquals(expectedType, transaction.getType());
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertNotNull(transaction.getFailureReason());
        assertFalse(transaction.getFailureReason().isBlank());
    }




}
