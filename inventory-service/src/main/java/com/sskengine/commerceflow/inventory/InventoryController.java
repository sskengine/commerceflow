package com.sskengine.commerceflow.inventory;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


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
