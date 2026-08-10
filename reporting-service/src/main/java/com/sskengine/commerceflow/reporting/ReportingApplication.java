package com.sskengine.commerceflow.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
public class ReportingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingApplication.class, args);
    }
}

@Entity
@Table(name = "processed_event")
class ProcessedEventEntity {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {}

    ProcessedEventEntity(UUID eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }
}

@Entity
@Table(name = "event_counter")
class EventCounterEntity {
    @Id
    private String eventType;
    @Column(nullable = false)
    private long eventCount;

    protected EventCounterEntity() {}

    EventCounterEntity(String eventType, long eventCount) {
        this.eventType = eventType;
        this.eventCount = eventCount;
    }

    String getEventType() { return eventType; }
    long getEventCount() { return eventCount; }

    void increment() {
        eventCount++;
    }
}

@Repository
interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {}

@Repository
interface EventCounterRepository extends JpaRepository<EventCounterEntity, String> {}

/** FR-080, INT-006: idempotent operational projection. */
@Service
class ReportingService {
    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final ProcessedEventRepository processed;
    private final EventCounterRepository counters;
    private final ObjectMapper mapper;

    ReportingService(ProcessedEventRepository processed, EventCounterRepository counters, ObjectMapper mapper) {
        this.processed = processed;
        this.counters = counters;
        this.mapper = mapper;
    }

    @Transactional
    void project(UUID eventId, String eventType) {
        if (processed.existsById(eventId)) {
            return;
        }
        processed.save(new ProcessedEventEntity(eventId, eventType, Instant.now()));
        EventCounterEntity counter = counters.findById(eventType)
                .orElseGet(() -> new EventCounterEntity(eventType, 0));
        counter.increment();
        counters.save(counter);
    }

    Map<String, Long> operations() {
        return counters.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(EventCounterEntity::getEventType, EventCounterEntity::getEventCount));
    }

    @KafkaListener(topics = {
            CommerceTopics.ORDER_CREATED,
            CommerceTopics.ORDER_STATUS_CHANGED,
            CommerceTopics.STOCK_RESERVED,
            CommerceTopics.PAYMENT_AUTHORIZED,
            CommerceTopics.PAYMENT_FAILED,
            CommerceTopics.SHIPMENT_CREATED,
            CommerceTopics.REFUND_COMPLETED
    })
    public void onEvent(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            project(UUID.fromString(root.path("eventId").asText()), root.path("eventType").asText());
        } catch (Exception ex) {
            log.warn("Failed to project event: {}", ex.getMessage());
        }
    }
}

@RestController
@RequestMapping("/api/v1/reports")
class ReportingController {
    private final ReportingService service;

    ReportingController(ReportingService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void project(@RequestBody EventEnvelope<Object> event) {
        service.project(event.eventId(), event.eventType());
    }

    @GetMapping("/operations")
    Map<String, Long> operations() {
        return service.operations();
    }
}
