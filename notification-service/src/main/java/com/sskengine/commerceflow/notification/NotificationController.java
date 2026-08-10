package com.sskengine.commerceflow.notification;

import com.sskengine.commerceflow.contracts.EventEnvelope;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


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
