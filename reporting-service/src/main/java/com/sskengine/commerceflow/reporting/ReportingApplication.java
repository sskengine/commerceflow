package com.sskengine.commerceflow.reporting;

import com.sskengine.commerceflow.contracts.EventEnvelope;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ReportingApplication {
    public static void main(String[] args) { SpringApplication.run(ReportingApplication.class, args); }
}

/** FR-080, INT-006: idempotent operational projection. */
@RestController
@RequestMapping("/api/v1/reports")
class ReportingController {
    private final Map<UUID, Boolean> processed = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> eventCounts = new ConcurrentHashMap<>();

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void project(@RequestBody EventEnvelope<Object> event) {
        if (processed.putIfAbsent(event.eventId(), Boolean.TRUE) == null) {
            eventCounts.computeIfAbsent(event.eventType(), ignored -> new LongAdder()).increment();
        }
    }

    @GetMapping("/operations")
    Map<String, Long> operations() {
        return eventCounts.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().sum()));
    }
}

