package com.sskengine.commerceflow.order;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateOrderRequestTest {
    @Test void currencyIsMandatory() {
        var request = new CreateOrderRequest(UUID.randomUUID(), null,
                List.of(new OrderItemRequest(UUID.randomUUID(), 1, BigDecimal.ONE)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }
}

