package com.company.wallet.service;

import com.company.wallet.domain.Money;
import com.company.wallet.domain.Transaction;
import com.company.wallet.domain.TransactionStatus;
import com.company.wallet.domain.Wallet;
import com.company.wallet.exception.InvalidAmountException;
import com.company.wallet.exception.SelfTransferException;
import com.company.wallet.exception.WalletInactiveException;
import com.company.wallet.exception.WalletNotFoundException;
import com.company.wallet.repository.TransactionRepository;
import com.company.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class WalletService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }
    public synchronized Wallet createWallet(String customerId, String currency){
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId không hợp lệ!");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tiền tệ không hợp lệ!");
        }
        String walletId = UUID.randomUUID().toString();
        Wallet wallet = new Wallet(walletId, customerId.trim(), currency.trim().toUpperCase());
        walletRepository.save(wallet);
        return wallet;
    }
    private Wallet getWalletOrThrow(String walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new WalletNotFoundException("Không tìm thấy ví: " + walletId));
    }
    public Money getBalance(String walletId){
        Wallet wallet = getWalletOrThrow(walletId);
        return wallet.getBalance();
    }

    public synchronized Transaction deposit(String walletId, Money amount){
        try {
            Wallet wallet = getWalletOrThrow(walletId);
            validateActive(wallet);
            validateAmount(amount);
            wallet.deposit(amount);
            walletRepository.save(wallet);
        }catch (RuntimeException ex){
            saveFailedDeposit(walletId, amount, ex);
            throw ex;
        }
        Transaction transaction = Transaction.forDeposit(walletId,amount,TransactionStatus.SUCCESS,null);
        transactionRepository.save(transaction);
        return transaction;
    }

    public synchronized Transaction withdraw(String walletId, Money amount) {
        Wallet wallet;
        try {
            wallet = getWalletOrThrow(walletId);
            validateActive(wallet);
            validateAmount(amount);
            wallet.withdraw(amount);
        } catch (RuntimeException ex) {
            saveFailedWithdraw(walletId, amount, ex);
            throw ex;
        }
        walletRepository.save(wallet);
        Transaction transaction = Transaction.forWithdraw(walletId, amount, TransactionStatus.SUCCESS, null);
        transactionRepository.save(transaction);
        return transaction;
    }

    private void validateActive(Wallet wallet) {
        if (!wallet.getStatus().canTransact()) {
            throw new WalletInactiveException("Ví không ở trạng thái hoạt động!");
        }
    }

    public synchronized Transaction transfer(String sourceWalletId, String destinationWalletId, Money amount) {
        Wallet sourceWallet = null;
        Wallet destinationWallet = null;
        boolean sourceDebited = false;
        try {
            if (sourceWalletId != null && sourceWalletId.equals(destinationWalletId)) {
                throw new SelfTransferException("Không thể chuyển tiền cho chính mình!");
            }
            sourceWallet = getWalletOrThrow(sourceWalletId);
            destinationWallet = getWalletOrThrow(destinationWalletId);
            validateActive(sourceWallet);
            validateActive(destinationWallet);
            validateAmount(amount);
            sourceWallet.withdraw(amount);
            sourceDebited = true;
            try {
                destinationWallet.deposit(amount);
            } catch (RuntimeException ex) {
                sourceWallet.deposit(amount);
                sourceDebited = false;
                throw ex;
            }
        } catch (RuntimeException ex) {
            if (sourceDebited) {
                try {
                    sourceWallet.deposit(amount);
                } catch (RuntimeException rollbackException) {
                    ex.addSuppressed(rollbackException);
                }
            }
            saveFailedTransfer(sourceWalletId, destinationWalletId, amount, ex);
            throw ex;
        }
        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);
        Transaction transaction = Transaction.forTransfer(sourceWalletId, destinationWalletId, amount, TransactionStatus.SUCCESS, null);
        transactionRepository.save(transaction);
        return transaction;
    }

    private void validateAmount(Money amount) {
        if (amount == null || amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Số tiền giao dịch phải lớn hơn 0!");
        }
    }

    private boolean isValidWalletId(String walletId) {
        return walletId != null && !walletId.isBlank();
    }

    private String getFailureReason(RuntimeException ex) {
        return ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private void saveFailedDeposit(String walletId, Money amount, RuntimeException ex) {
        if (!isValidWalletId(walletId) || amount == null) {
            return;
        }
        transactionRepository.save(Transaction.forDeposit(walletId, amount, TransactionStatus.FAILED, getFailureReason(ex)));
    }

    private void saveFailedWithdraw(String walletId, Money amount, RuntimeException ex) {
        if (!isValidWalletId(walletId) || amount == null) {
            return;
        }
        transactionRepository.save(Transaction.forWithdraw(walletId, amount, TransactionStatus.FAILED, getFailureReason(ex)));
    }

    private void saveFailedTransfer(String sourceWalletId, String destinationWalletId, Money amount, RuntimeException ex) {
        if (!isValidWalletId(sourceWalletId)  || !isValidWalletId(destinationWalletId) || amount == null) {
            return;
        }
        transactionRepository.save(Transaction.forTransfer(sourceWalletId, destinationWalletId, amount, TransactionStatus.FAILED, getFailureReason(ex)));
    }

    public List<Transaction> getTransactionHistory(String walletId) {
        getWalletOrThrow(walletId);
        return transactionRepository.findByWalletId(walletId);
    }
}

