package com.sskengine.commerceflow.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ReservationServiceTest {
    private static final UUID PRODUCT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    ReservationService service;

    @Autowired
    StockItemRepository stock;

    @MockBean
    DomainEventPublisher events;

    @BeforeEach
    void seed() {
        stock.deleteAll();
        stock.save(new StockItemEntity(PRODUCT, 10));
    }

    @Test
    void preventsOverselling() {
        service.reserve(new ReserveStockRequest(UUID.randomUUID(), PRODUCT, 10), "corr-test-1");
        assertThrows(InsufficientStockException.class,
                () -> service.reserve(new ReserveStockRequest(UUID.randomUUID(), PRODUCT, 1), "corr-test-2"));
    }
}
