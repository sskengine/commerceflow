package com.sskengine.commerceflow.payment;

import static org.junit.jupiter.api.Assertions.*;
import com.sskengine.commerceflow.contracts.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test void repeatedKeyReturnsOriginalPayment() {
        PaymentService service = new PaymentService();
        PaymentRequest request = new PaymentRequest(UUID.randomUUID(), new Money(new BigDecimal("25"), "USD"), "tok_demo");
        PaymentView first = service.authorize(request, "same-key", "corr-12345678");
        PaymentView second = service.authorize(request, "same-key", "corr-12345678");
        assertEquals(first.paymentId(), second.paymentId());
    }
}
