package com.sskengine.commerceflow.inventory;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
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

class StockItemEntity {
    private UUID productId;
    private int availableQty;

    StockItemEntity() {}

    StockItemEntity(UUID productId, int availableQty) {
        this.productId = productId;
        this.availableQty = availableQty;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }

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

class ReservationEntity {
    private UUID reservationId;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private Instant expiresAt;
    private String status;

    ReservationEntity() {}

    ReservationEntity(UUID reservationId, UUID orderId, UUID productId, int quantity, Instant expiresAt, String status) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    void release() {
        this.status = "RELEASED";
    }

    ReservationView toView() {
        return new ReservationView(reservationId, orderId, productId, quantity, expiresAt, status);
    }
}

@Mapper
interface StockItemRepository {
    @Select("SELECT product_id, available_qty FROM stock_item WHERE product_id = #{productId} FOR UPDATE")
    StockItemEntity findForUpdate(@Param("productId") UUID productId);

    @Insert("INSERT INTO stock_item (product_id, available_qty) VALUES (#{productId}, #{availableQty})")
    void insert(StockItemEntity item);

    @Update("UPDATE stock_item SET available_qty = #{availableQty} WHERE product_id = #{productId}")
    void update(StockItemEntity item);

    @Delete("DELETE FROM stock_item")
    void deleteAll();
}

@Mapper
interface ReservationRepository {
    @Insert("""
            INSERT INTO reservation (reservation_id, order_id, product_id, quantity, expires_at, status)
            VALUES (#{reservationId}, #{orderId}, #{productId}, #{quantity}, #{expiresAt}, #{status})
            """)
    void insert(ReservationEntity reservation);

    @Select("""
            SELECT reservation_id, order_id, product_id, quantity, expires_at, status
            FROM reservation
            WHERE order_id = #{orderId} AND status = #{status}
            """)
    List<ReservationEntity> findByOrderIdAndStatus(@Param("orderId") UUID orderId, @Param("status") String status);

    @Update("UPDATE reservation SET status = #{status} WHERE reservation_id = #{reservationId}")
    void updateStatus(ReservationEntity reservation);

    @Delete("DELETE FROM reservation")
    void deleteAll();
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
