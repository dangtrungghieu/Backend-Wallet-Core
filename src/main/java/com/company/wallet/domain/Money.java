package com.company.wallet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;
    private final String currency;
    private static final RoundingMode roundingMode = RoundingMode.HALF_UP;

    public Money(double amount, String currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số dư không hợp lệ!");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tiền tệ không hợp lệ!");
        }
        this.amount = amount.setScale(2, roundingMode);
        this.currency = currency.trim().toUpperCase();
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        BigDecimal newAmount = this.amount.add(other.amount).setScale(2, roundingMode);
        return new Money(newAmount, this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException("Số dư không đủ!");
        }
        BigDecimal newAmount = this.amount.subtract(other.amount).setScale(2, roundingMode);
       return new Money(newAmount, other.currency);
    }

    private void validateSameCurrency(Money other){
        Objects.requireNonNull(other, "Khoản tiền không được để trống!");
        if (!Objects.equals(this.currency, other.currency)) {
            throw new IllegalArgumentException("Không thể thao tác giữa hai loại tiền tệ khác nhau: " + this.currency + " và " + other.currency);
        }
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Money money = (Money) obj;
        return Objects.equals(this.currency,money.currency) && this.amount.compareTo(money.amount) == 0;
    }
    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(),this.currency);
    }

    @Override
    public String toString() {
        return this.amount.stripTrailingZeros().toPlainString() + " " + this.currency;
    }

}
