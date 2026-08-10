package com.sskengine.commerceflow.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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
public class ReportingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingApplication.class, args);
    }
}

class ProcessedEventEntity {
    private UUID eventId;
    private String eventType;
    private Instant processedAt;

    ProcessedEventEntity() {}

    ProcessedEventEntity(UUID eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}

class EventCounterEntity {
    private String eventType;
    private long eventCount;

    EventCounterEntity() {}

    EventCounterEntity(String eventType, long eventCount) {
        this.eventType = eventType;
        this.eventCount = eventCount;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }

    void increment() {
        eventCount++;
    }
}

@Mapper
interface ProcessedEventRepository {
    @Select("SELECT COUNT(*) FROM processed_event WHERE event_id = #{eventId}")
    int countById(@Param("eventId") UUID eventId);

    @Insert("""
            INSERT INTO processed_event (event_id, event_type, processed_at)
            VALUES (#{eventId}, #{eventType}, #{processedAt})
            """)
    void insert(ProcessedEventEntity event);
}

@Mapper
interface EventCounterRepository {
    @Select("SELECT event_type, event_count FROM event_counter WHERE event_type = #{eventType}")
    EventCounterEntity findById(@Param("eventType") String eventType);

    @Insert("INSERT INTO event_counter (event_type, event_count) VALUES (#{eventType}, #{eventCount})")
    void insert(EventCounterEntity counter);

    @Update("UPDATE event_counter SET event_count = #{eventCount} WHERE event_type = #{eventType}")
    void update(EventCounterEntity counter);

    @Select("SELECT event_type, event_count FROM event_counter")
    java.util.List<EventCounterEntity> findAll();
}

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
        if (processed.countById(eventId) > 0) {
            return;
        }
        processed.insert(new ProcessedEventEntity(eventId, eventType, Instant.now()));
        EventCounterEntity counter = counters.findById(eventType);
        if (counter == null) {
            counter = new EventCounterEntity(eventType, 0);
            counter.increment();
            counters.insert(counter);
        } else {
            counter.increment();
            counters.update(counter);
        }
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
