package com.sskengine.commerceflow.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
