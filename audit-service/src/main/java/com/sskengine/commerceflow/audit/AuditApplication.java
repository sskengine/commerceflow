package com.sskengine.commerceflow.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class AuditApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}

@Entity
@Table(name = "audit_entry")
class AuditEntryEntity {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private String actorId;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String resourceType;
    @Column(nullable = false)
    private String resourceId;
    @Column(nullable = false)
    private String correlationId;
    @Column(nullable = false)
    private Instant occurredAt;

    protected AuditEntryEntity() {}

    AuditEntryEntity(UUID eventId, String actorId, String action, String resourceType, String resourceId,
                     String correlationId, Instant occurredAt) {
        this.eventId = eventId;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    AuditEntry toView() {
        return new AuditEntry(eventId, actorId, action, resourceType, resourceId, correlationId, occurredAt);
    }
}

@Repository
interface AuditRepository extends JpaRepository<AuditEntryEntity, UUID> {}

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
        return audits.save(entity).toView();
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

@RestController
@RequestMapping("/api/v1/audit-events")
class AuditController {
    private final AuditService service;

    AuditController(AuditService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    AuditEntry append(@Valid @RequestBody AuditCommand command) {
        return service.append(command);
    }

    @GetMapping
    List<AuditEntry> list() {
        return service.list();
    }
}

record AuditCommand(@NotBlank String actorId, @NotBlank String action, @NotBlank String resourceType,
                    @NotBlank String resourceId, @NotBlank String correlationId) {}
record AuditEntry(UUID eventId, String actorId, String action, String resourceType,
                  String resourceId, String correlationId, Instant occurredAt) {}
