package com.sskengine.commerceflow.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class InventoryApplication {
    public static void main(String[] args) { SpringApplication.run(InventoryApplication.class, args); }
}

/** BR-002, FR-030, BRULE-006, AC-003. */
@Service
class ReservationService {
    private final Map<UUID, AtomicInteger> available = new ConcurrentHashMap<>();
    private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();

    ReservationService() {
        available.put(UUID.fromString("22222222-2222-2222-2222-222222222222"), new AtomicInteger(10));
        available.put(UUID.fromString("33333333-3333-3333-3333-333333333333"), new AtomicInteger(5));
    }

    Reservation reserve(ReserveStockRequest request) {
        AtomicInteger stock = available.computeIfAbsent(request.productId(), ignored -> new AtomicInteger());
        while (true) {
            int current = stock.get();
            if (current < request.quantity()) throw new InsufficientStockException();
            if (stock.compareAndSet(current, current - request.quantity())) break;
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), request.orderId(), request.productId(),
                request.quantity(), Instant.now().plus(15, ChronoUnit.MINUTES), "CONFIRMED");
        reservations.put(reservation.reservationId(), reservation);
        return reservation;
    }
}

@RestController
@RequestMapping("/api/v1/reservations")
class InventoryController {
    private final ReservationService service;
    InventoryController(ReservationService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Reservation reserve(@Valid @RequestBody ReserveStockRequest request) { return service.reserve(request); }
}

record ReserveStockRequest(@NotNull UUID orderId, @NotNull UUID productId, @Positive int quantity) {}
record Reservation(UUID reservationId, UUID orderId, UUID productId, int quantity, Instant expiresAt, String status) {}

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "INSUFFICIENT_STOCK")
class InsufficientStockException extends RuntimeException {}

