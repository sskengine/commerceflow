package com.sskengine.commerceflow.notification;

import com.sskengine.commerceflow.contracts.EventEnvelope;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) { SpringApplication.run(NotificationApplication.class, args); }
}

/** FR-060, INT-006: repeated event delivery is safe. */
@RestController
@RequestMapping("/internal/v1/notifications")
class NotificationController {
    private final Set<UUID> processedEvents = ConcurrentHashMap.newKeySet();

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    NotificationResult consume(@Valid @RequestBody EventEnvelope<Object> event) {
        boolean firstDelivery = processedEvents.add(event.eventId());
        return new NotificationResult(event.eventId(), firstDelivery ? "QUEUED" : "DUPLICATE_IGNORED", Instant.now());
    }
}

record NotificationResult(UUID eventId, String status, Instant processedAt) {}

