package com.sskengine.commerceflow.inventory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {
    private static final UUID PRODUCT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test void preventsOverselling() {
        ReservationService service = new ReservationService();
        service.reserve(new ReserveStockRequest(UUID.randomUUID(), PRODUCT, 10));
        assertThrows(InsufficientStockException.class,
                () -> service.reserve(new ReserveStockRequest(UUID.randomUUID(), PRODUCT, 1)));
    }
}

