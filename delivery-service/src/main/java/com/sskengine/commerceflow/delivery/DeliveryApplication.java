package com.sskengine.commerceflow.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DeliveryApplication {
    public static void main(String[] args) { SpringApplication.run(DeliveryApplication.class, args); }
}

/** FR-050: shipment lifecycle boundary. */
@RestController
@RequestMapping("/api/v1/shipments")
class DeliveryController {
    private final Map<UUID, Shipment> shipments = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Shipment create(@Valid @RequestBody CreateShipment request) {
        Shipment shipment = new Shipment(UUID.randomUUID(), request.orderId(), request.addressRef(),
                "CF-" + UUID.randomUUID().toString().substring(0, 8), "CREATED", Instant.now());
        shipments.put(shipment.shipmentId(), shipment);
        return shipment;
    }

    @GetMapping("/{id}") Shipment get(@PathVariable UUID id) {
        Shipment shipment = shipments.get(id);
        if (shipment == null) throw new ShipmentNotFoundException();
        return shipment;
    }
}

record CreateShipment(@NotNull UUID orderId, @NotBlank String addressRef) {}
record Shipment(UUID shipmentId, UUID orderId, String addressRef, String trackingNumber, String status, Instant createdAt) {}
@ResponseStatus(HttpStatus.NOT_FOUND) class ShipmentNotFoundException extends RuntimeException {}

