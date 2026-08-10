package com.sskengine.commerceflow.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-060, INT-006: repeated event delivery is safe. */
@Service
class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final ObjectMapper mapper;

    NotificationService(NotificationRepository notifications, ObjectMapper mapper) {
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @Transactional
    NotificationResult accept(UUID eventId, String eventType, String correlationId) {
        if (notifications.countById(eventId) > 0) {
            return new NotificationResult(eventId, "DUPLICATE_IGNORED", Instant.now());
        }
        NotificationEntity entity = new NotificationEntity(eventId, eventType, correlationId, "QUEUED", Instant.now());
        notifications.insert(entity);
        return entity.toResult();
    }

    List<NotificationView> list() {
        return notifications.findAll().stream().map(NotificationEntity::toView).toList();
    }

    @KafkaListener(topics = {
            CommerceTopics.ORDER_CREATED,
            CommerceTopics.PAYMENT_AUTHORIZED,
            CommerceTopics.PAYMENT_FAILED,
            CommerceTopics.SHIPMENT_CREATED
    })
    public void onEvent(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            accept(UUID.fromString(root.path("eventId").asText()),
                    root.path("eventType").asText(),
                    root.path("correlationId").asText("notification-kafka"));
        } catch (Exception ex) {
            log.warn("Failed to consume notification event: {}", ex.getMessage());
        }
    }
}
