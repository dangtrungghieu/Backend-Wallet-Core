package com.company.wallet.domain;

import com.company.wallet.exception.WalletNotFoundException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
    private final String transactionId;
    private final String sourceWalletId;
    private final String destinationWalletId;
    private final Money amount;
    private final TransactionType type;
    private final TransactionStatus status;
    private final String failureReason;
    private final LocalDateTime createdAt;

    private Transaction(
            String sourceWalletId,
            String destinationWalletId,
            Money amount,
            TransactionType transactionType,
            TransactionStatus status,
            String failureReason
    ){
        this.transactionId = UUID.randomUUID().toString();
        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;
        this.amount = Objects.requireNonNull(amount,"Số tiền không được NULL!");
        this.type = Objects.requireNonNull(transactionType);
        this.status = Objects.requireNonNull(status);
        if (this.status == TransactionStatus.FAILED && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("Thiếu lý do thất bại!");
        }
        this.failureReason = status == TransactionStatus.SUCCESS ? null : failureReason;
        this.createdAt = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getSourceWalletId() {
        return sourceWalletId;
    }

    public String getDestinationWalletId() {
        return destinationWalletId;
    }

    public Money getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static Transaction forDeposit(
            String destinationWalletId,
            Money amount,
            TransactionStatus status,
            String failureReason
    ){
        if (destinationWalletId == null || destinationWalletId.isBlank()) {
            throw new IllegalArgumentException("WalletId nhận không được bỏ trống!");
        }
        return new Transaction(
                null,
                destinationWalletId,
                amount,
                TransactionType.DEPOSIT,
                status,
                failureReason
        );
    }
    public static Transaction forWithdraw(
            String sourceWalletId,
            Money amount,
            TransactionStatus status,
            String failureReason
    ) {
        if (sourceWalletId == null || sourceWalletId.isBlank()) {
            throw new IllegalArgumentException("WalletId gửi không được bỏ trống!");
        }
        return new Transaction(
                sourceWalletId,
                null,
                amount,
                TransactionType.WITHDRAW,
                status,
                failureReason
        );
    }

    public static Transaction forTransfer(
            String sourceWalletId,
            String destinationWalletId,
            Money amount,
            TransactionStatus status,
            String failureReason
    ) {
        if (sourceWalletId == null || sourceWalletId.isBlank()) {
            throw new IllegalArgumentException("WalletId gửi không được bỏ trống!");
        }
        if (destinationWalletId == null || destinationWalletId.isBlank()) {
            throw new IllegalArgumentException("WalletId nhận không được bỏ trống!");
        }
        return new Transaction(
                sourceWalletId,
                destinationWalletId,
                amount,
                TransactionType.TRANSFER,
                status,
                failureReason
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    @Override
    public String toString() {
        return this.transactionId + ": " + this.amount + " " + this.type + " " + this.status + " " + this.failureReason;
    }
}
