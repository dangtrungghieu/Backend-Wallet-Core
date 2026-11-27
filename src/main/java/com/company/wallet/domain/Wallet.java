package com.company.wallet.domain;

import com.company.wallet.exception.InsufficientBalanceException;
import com.company.wallet.exception.InvalidAmountException;
import com.company.wallet.exception.WalletInactiveException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Wallet {
    private final String walletId;
    private final String customerId;
    private Money balance;
    private WalletStatus status;
    private final LocalDateTime createdAt;

    public Wallet(String walletId, String customerId, String currency) {

        if (walletId == null || walletId.isBlank()) {
            throw new IllegalArgumentException("Wallet ID không được để trống!");
        }

        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID không được để trống!");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency không được để trống!");
        }

        this.walletId = walletId.trim();
        this.customerId = customerId.trim();
        this.balance = new Money(BigDecimal.ZERO, currency.trim().toUpperCase());
        this.status = WalletStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public String getWalletId() {
        return walletId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    private void validateTransactionAmount(Money amount){
        if(!status.canTransact()){
            throw new WalletInactiveException("Ví đang bị khóa!");
        }
        if(amount == null || amount.getAmount().compareTo(BigDecimal.ZERO) <= 0){
            throw new InvalidAmountException("Số tiền nạp phải lớn hơn 0!");
        }
    }

    public void deposit(Money amount) {
        validateTransactionAmount(amount);
        this.balance = this.balance.add(amount);
    }

    public void withdraw(Money amount) {
        validateTransactionAmount(amount);
        if(!this.balance.getCurrency().equals(amount.getCurrency())){
            throw new IllegalArgumentException("Khác đơn vị tiền tệ!");
        }
        if(!this.balance.isGreaterThanOrEqual(amount)){
            throw new InsufficientBalanceException("Số dư không đủ để thực hiện giao dịch!");
        }
        this.balance = this.balance.subtract(amount);

    }
    public void freeze(){
        this.status = WalletStatus.FROZEN;
    }
    public void activate() {
        if(this.status == WalletStatus.CLOSED){
            throw new WalletInactiveException("Không thể kích hoạt lại ví đã bị đóng!");
        }
        this.status = WalletStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return walletId.equals(wallet.walletId);
    }
    @Override
    public int hashCode() {
        return walletId.hashCode();
    }
}
