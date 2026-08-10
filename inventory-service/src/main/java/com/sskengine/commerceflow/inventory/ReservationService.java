package com.sskengine.commerceflow.inventory;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-002, FR-030, BRULE-006, AC-003: transactional reservation with Kafka signal. */
@Service
class ReservationService {
    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    private final DomainEventPublisher events;

    ReservationService(StockItemRepository stock, ReservationRepository reservations, DomainEventPublisher events) {
        this.stock = stock;
        this.reservations = reservations;
        this.events = events;
    }

    @Transactional
    ReservationView reserve(ReserveStockRequest request, String correlationId) {
        StockItemEntity item = stock.findForUpdate(request.productId());
        if (item == null) {
            throw new InsufficientStockException();
        }
        item.decrease(request.quantity());
        stock.update(item);
        ReservationEntity reservation = new ReservationEntity(
                UUID.randomUUID(), request.orderId(), request.productId(), request.quantity(),
                Instant.now().plus(15, ChronoUnit.MINUTES), "CONFIRMED");
        reservations.insert(reservation);
        events.publish(CommerceTopics.STOCK_RESERVED, EventEnvelope.v1(
                "StockReserved",
                correlationId,
                new CommerceEvents.StockReserved(reservation.getReservationId(), reservation.getOrderId(), reservation.getExpiresAt())));
        return reservation.toView();
    }

    @Transactional
    int releaseByOrder(UUID orderId, String correlationId) {
        List<ReservationEntity> active = reservations.findByOrderIdAndStatus(orderId, "CONFIRMED");
        for (ReservationEntity reservation : active) {
            StockItemEntity item = stock.findForUpdate(reservation.getProductId());
            if (item != null) {
                item.increase(reservation.getQuantity());
                stock.update(item);
            }
            reservation.release();
            reservations.updateStatus(reservation);
        }
        return active.size();
    }
}
