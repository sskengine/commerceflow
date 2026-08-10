package com.sskengine.commerceflow.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}

class NotificationEntity {
    private UUID eventId;
    private String eventType;
    private String correlationId;
    private String status;
    private Instant processedAt;

    NotificationEntity() {}

    NotificationEntity(UUID eventId, String eventType, String correlationId, String status, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.status = status;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    NotificationResult toResult() {
        return new NotificationResult(eventId, status, processedAt);
    }

    NotificationView toView() {
        return new NotificationView(eventId, eventType, correlationId, status, processedAt);
    }
}

@Mapper
interface NotificationRepository {
    @Select("SELECT COUNT(*) FROM notification_message WHERE event_id = #{eventId}")
    int countById(@Param("eventId") UUID eventId);

    @Insert("""
            INSERT INTO notification_message (event_id, event_type, correlation_id, status, processed_at)
            VALUES (#{eventId}, #{eventType}, #{correlationId}, #{status}, #{processedAt})
            """)
    void insert(NotificationEntity entity);

    @Select("""
            SELECT event_id, event_type, correlation_id, status, processed_at
            FROM notification_message
            """)
    List<NotificationEntity> findAll();
}

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

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {
    private final NotificationService service;

    NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    NotificationResult consume(@Valid @RequestBody EventEnvelope<Object> event) {
        return service.accept(event.eventId(), event.eventType(), event.correlationId());
    }

    @GetMapping
    List<NotificationView> list() {
        return service.list();
    }
}

record NotificationResult(UUID eventId, String status, Instant processedAt) {}
record NotificationView(UUID eventId, String eventType, String correlationId, String status, Instant processedAt) {}
