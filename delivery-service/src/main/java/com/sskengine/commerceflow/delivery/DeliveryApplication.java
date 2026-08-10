package com.sskengine.commerceflow.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
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

class ShipmentEntity {
    private UUID shipmentId;
    private UUID orderId;
    private String addressRef;
    private String trackingNumber;
    private String status;
    private String providerReference;
    private String correlationId;
    private Instant createdAt;

    ShipmentEntity() {}

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

    public UUID getShipmentId() { return shipmentId; }
    public void setShipmentId(UUID shipmentId) { this.shipmentId = shipmentId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getAddressRef() { return addressRef; }
    public void setAddressRef(String addressRef) { this.addressRef = addressRef; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    void updateStatus(String status) {
        this.status = status;
    }

    ShipmentView toView() {
        return new ShipmentView(shipmentId, orderId, addressRef, trackingNumber, status, createdAt);
    }
}

@Mapper
interface ShipmentRepository {
    @Select("""
            SELECT shipment_id, order_id, address_ref, tracking_number, status, provider_reference,
                   correlation_id, created_at
            FROM shipment
            WHERE order_id = #{orderId}
            """)
    ShipmentEntity findByOrderId(@Param("orderId") UUID orderId);

    @Select("""
            SELECT shipment_id, order_id, address_ref, tracking_number, status, provider_reference,
                   correlation_id, created_at
            FROM shipment
            WHERE shipment_id = #{id}
            """)
    ShipmentEntity findById(@Param("id") UUID id);

    @Insert("""
            INSERT INTO shipment (shipment_id, order_id, address_ref, tracking_number, status,
                                  provider_reference, correlation_id, created_at)
            VALUES (#{shipmentId}, #{orderId}, #{addressRef}, #{trackingNumber}, #{status},
                    #{providerReference}, #{correlationId}, #{createdAt})
            """)
    void insert(ShipmentEntity shipment);

    @Update("UPDATE shipment SET status = #{status} WHERE shipment_id = #{shipmentId}")
    void updateStatus(ShipmentEntity shipment);
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
        ShipmentEntity existing = shipments.findByOrderId(request.orderId());
        if (existing != null) {
            return existing.toView();
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
        shipments.insert(shipment);
        events.publish(CommerceTopics.SHIPMENT_CREATED, EventEnvelope.v1(
                "ShipmentCreated", correlationId,
                new CommerceEvents.ShipmentCreated(shipmentId, request.orderId(), tracking)));
        return shipment.toView();
    }

    ShipmentView get(UUID id) {
        ShipmentEntity shipment = shipments.findById(id);
        if (shipment == null) {
            throw new ShipmentNotFoundException();
        }
        return shipment.toView();
    }

    @Transactional
    ShipmentView updateStatus(UUID id, UpdateStatusRequest request) {
        ShipmentEntity shipment = shipments.findById(id);
        if (shipment == null) {
            throw new ShipmentNotFoundException();
        }
        shipment.updateStatus(request.status());
        shipments.updateStatus(shipment);
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
