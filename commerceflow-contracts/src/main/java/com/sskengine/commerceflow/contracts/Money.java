package com.sskengine.commerceflow.contracts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** DATA-001, BRULE-002, BRULE-004: fixed precision money with ISO 4217 currency. */
public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Currency.getInstance(currency);
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (amount.signum() < 0) throw new IllegalArgumentException("amount must not be negative");
    }

    public Money multiply(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
}

