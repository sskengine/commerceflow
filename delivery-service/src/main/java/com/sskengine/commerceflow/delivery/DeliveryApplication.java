package com.sskengine.commerceflow.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {
        "com.sskengine.commerceflow.delivery",
        "com.sskengine.commerceflow.messaging"
})
public class DeliveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }

    @Bean
    RestClient providerClient(@Value("${clients.provider.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}

@Entity
@Table(name = "shipment")
class ShipmentEntity {
    @Id
    private UUID shipmentId;
    @Column(nullable = false, unique = true)
    private UUID orderId;
    @Column(nullable = false)
    private String addressRef;
    @Column(nullable = false)
    private String trackingNumber;
    @Column(nullable = false)
    private String status;
    private String providerReference;
    @Column(nullable = false)
    private String correlationId;
    @Column(nullable = false)
    private Instant createdAt;

    protected ShipmentEntity() {}

    ShipmentEntity(UUID shipmentId, UUID orderId, String addressRef, String trackingNumber, String status,
                   String providerReference, String correlationId, Instant createdAt) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.addressRef = addressRef;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.providerReference = providerReference;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    UUID getShipmentId() { return shipmentId; }
    UUID getOrderId() { return orderId; }
    String getTrackingNumber() { return trackingNumber; }
    String getStatus() { return status; }

    void updateStatus(String status) {
        this.status = status;
    }

    ShipmentView toView() {
        return new ShipmentView(shipmentId, orderId, addressRef, trackingNumber, status, createdAt);
    }
}

@Repository
interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {
    Optional<ShipmentEntity> findByOrderId(UUID orderId);
}

/** FR-050: shipment lifecycle; reacts to authorized payments. */
@Service
class DeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final ShipmentRepository shipments;
    private final RestClient provider;
    private final DomainEventPublisher events;
    private final ObjectMapper mapper;

    DeliveryService(ShipmentRepository shipments, RestClient providerClient, DomainEventPublisher events,
                    ObjectMapper mapper) {
        this.shipments = shipments;
        this.provider = providerClient;
        this.events = events;
        this.mapper = mapper;
    }

    @Transactional
    ShipmentView create(CreateShipmentRequest request, String correlationId) {
        Optional<ShipmentEntity> existing = shipments.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            return existing.get().toView();
        }

        UUID shipmentId = UUID.randomUUID();
        Map<?, ?> providerResponse = provider.post().uri("/stubs/v1/logistics/shipments")
                .header("X-Correlation-Id", correlationId)
                .body(Map.of("operationId", shipmentId.toString()))
                .retrieve().body(Map.class);
        String providerReference = providerResponse == null ? null : String.valueOf(providerResponse.get("providerReference"));
        String tracking = "CF-" + shipmentId.toString().substring(0, 8);

        ShipmentEntity shipment = new ShipmentEntity(shipmentId, request.orderId(), request.addressRef(), tracking,
                "CREATED", providerReference, correlationId, Instant.now());
        shipments.save(shipment);
        events.publish(CommerceTopics.SHIPMENT_CREATED, EventEnvelope.v1(
                "ShipmentCreated", correlationId,
                new CommerceEvents.ShipmentCreated(shipmentId, request.orderId(), tracking)));
        return shipment.toView();
    }

    ShipmentView get(UUID id) {
        return shipments.findById(id).map(ShipmentEntity::toView).orElseThrow(ShipmentNotFoundException::new);
    }

    @Transactional
    ShipmentView updateStatus(UUID id, UpdateStatusRequest request) {
        ShipmentEntity shipment = shipments.findById(id).orElseThrow(ShipmentNotFoundException::new);
        shipment.updateStatus(request.status());
        return shipment.toView();
    }

    @KafkaListener(topics = CommerceTopics.PAYMENT_AUTHORIZED)
    public void onPaymentAuthorized(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String correlationId = root.path("correlationId").asText("delivery-kafka");
            UUID orderId = UUID.fromString(root.path("payload").path("orderId").asText());
            create(new CreateShipmentRequest(orderId, "addr-auto"), correlationId);
        } catch (Exception ex) {
            log.warn("Failed to process PaymentAuthorized: {}", ex.getMessage());
        }
    }
}

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

record CreateShipmentRequest(@NotNull UUID orderId, @NotBlank String addressRef) {}
record UpdateStatusRequest(@NotBlank String status) {}
record ShipmentView(UUID shipmentId, UUID orderId, String addressRef, String trackingNumber, String status, Instant createdAt) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class ShipmentNotFoundException extends RuntimeException {}
