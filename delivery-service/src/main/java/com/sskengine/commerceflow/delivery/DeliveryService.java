package com.sskengine.commerceflow.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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
