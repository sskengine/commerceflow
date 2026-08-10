package com.sskengine.commerceflow.audit;

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

/** FR-090, SEC-005: append-only privileged operation history. */
@Service
class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository audits;
    private final ObjectMapper mapper;

    AuditService(AuditRepository audits, ObjectMapper mapper) {
        this.audits = audits;
        this.mapper = mapper;
    }

    @Transactional
    AuditEntry append(AuditCommand command) {
        AuditEntryEntity entity = new AuditEntryEntity(UUID.randomUUID(), command.actorId(), command.action(),
                command.resourceType(), command.resourceId(), command.correlationId(), Instant.now());
        audits.insert(entity);
        return entity.toView();
    }

    List<AuditEntry> list() {
        return audits.findAll().stream().map(AuditEntryEntity::toView).toList();
    }

    @KafkaListener(topics = {
            CommerceTopics.PAYMENT_AUTHORIZED,
            CommerceTopics.PAYMENT_FAILED,
            CommerceTopics.ORDER_STATUS_CHANGED,
            CommerceTopics.REFUND_COMPLETED
    })
    public void onDomainEvent(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String eventType = root.path("eventType").asText();
            String correlationId = root.path("correlationId").asText("audit-kafka");
            JsonNode payload = root.path("payload");
            String resourceId = payload.has("orderId")
                    ? payload.path("orderId").asText()
                    : payload.path("paymentId").asText("unknown");
            append(new AuditCommand("system", eventType, "domain-event", resourceId, correlationId));
        } catch (Exception ex) {
            log.warn("Failed to audit domain event: {}", ex.getMessage());
        }
    }
}
