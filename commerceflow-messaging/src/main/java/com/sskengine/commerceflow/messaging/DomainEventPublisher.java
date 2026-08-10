package com.sskengine.commerceflow.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;

/** Publishes versioned EventEnvelope payloads as JSON (INT-004). */
public class DomainEventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public DomainEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    public void publish(String topic, EventEnvelope<?> envelope) {
        try {
            String key = envelope.correlationId() == null ? envelope.eventId().toString() : envelope.correlationId();
            kafka.send(topic, key, mapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize event " + envelope.eventType(), ex);
        }
    }
}
