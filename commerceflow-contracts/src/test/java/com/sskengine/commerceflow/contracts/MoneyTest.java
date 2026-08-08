package com.sskengine.commerceflow.contracts;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test void rejectsUnknownCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.ONE, "XYZQ"));
    }

    @Test void usesFixedPrecision() {
        assertEquals(new BigDecimal("10.24"), new Money(new BigDecimal("10.235"), "USD").amount());
    }
}
