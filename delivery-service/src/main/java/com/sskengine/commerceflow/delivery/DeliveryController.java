package com.sskengine.commerceflow.delivery;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/shipments")
class DeliveryController {
    private final DeliveryService service;

    DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShipmentView create(@Valid @RequestBody CreateShipmentRequest request,
                        @RequestHeader(name = "X-Correlation-Id", defaultValue = "delivery-local") String correlationId) {
        return service.create(request, correlationId);
    }

    @GetMapping("/{id}")
    ShipmentView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}/status")
    ShipmentView updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
