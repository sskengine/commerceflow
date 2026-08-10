package com.sskengine.commerceflow.inventory;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {
        "com.sskengine.commerceflow.inventory",
        "com.sskengine.commerceflow.messaging"
})
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}

@Entity
@Table(name = "stock_item")
class StockItemEntity {
    @Id
    @Column(name = "product_id")
    private UUID productId;
    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    protected StockItemEntity() {}

    StockItemEntity(UUID productId, int availableQty) {
        this.productId = productId;
        this.availableQty = availableQty;
    }

    UUID getProductId() { return productId; }
    int getAvailableQty() { return availableQty; }

    void decrease(int quantity) {
        if (availableQty < quantity) {
            throw new InsufficientStockException();
        }
        availableQty -= quantity;
    }

    void increase(int quantity) {
        availableQty += quantity;
    }
}

@Entity
@Table(name = "reservation")
class ReservationEntity {
    @Id
    private UUID reservationId;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private Instant expiresAt;
    private String status;

    protected ReservationEntity() {}

    ReservationEntity(UUID reservationId, UUID orderId, UUID productId, int quantity, Instant expiresAt, String status) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    UUID getReservationId() { return reservationId; }
    UUID getOrderId() { return orderId; }
    UUID getProductId() { return productId; }
    int getQuantity() { return quantity; }
    Instant getExpiresAt() { return expiresAt; }
    String getStatus() { return status; }

    void release() {
        this.status = "RELEASED";
    }

    ReservationView toView() {
        return new ReservationView(reservationId, orderId, productId, quantity, expiresAt, status);
    }
}

@Repository
interface StockItemRepository extends JpaRepository<StockItemEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockItemEntity s where s.productId = :productId")
    StockItemEntity findForUpdate(@Param("productId") UUID productId);
}

@Repository
interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
    List<ReservationEntity> findByOrderIdAndStatus(UUID orderId, String status);
}

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
        ReservationEntity reservation = new ReservationEntity(
                UUID.randomUUID(), request.orderId(), request.productId(), request.quantity(),
                Instant.now().plus(15, ChronoUnit.MINUTES), "CONFIRMED");
        reservations.save(reservation);
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
            }
            reservation.release();
        }
        return active.size();
    }
}

@RestController
@RequestMapping("/api/v1/reservations")
class InventoryController {
    private final ReservationService service;

    InventoryController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReservationView reserve(@Valid @RequestBody ReserveStockRequest request,
                            @RequestHeader(name = "X-Correlation-Id", defaultValue = "inventory-local") String correlationId) {
        return service.reserve(request, correlationId);
    }

    @PostMapping("/release/{orderId}")
    ReleaseResult release(@PathVariable UUID orderId,
                          @RequestHeader(name = "X-Correlation-Id", defaultValue = "inventory-local") String correlationId) {
        return new ReleaseResult(orderId, service.releaseByOrder(orderId, correlationId));
    }
}

record ReserveStockRequest(@NotNull UUID orderId, @NotNull UUID productId, @Positive int quantity) {}
record ReservationView(UUID reservationId, UUID orderId, UUID productId, int quantity, Instant expiresAt, String status) {}
record ReleaseResult(UUID orderId, int releasedCount) {}

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "INSUFFICIENT_STOCK")
class InsufficientStockException extends RuntimeException {}
