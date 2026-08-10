package com.sskengine.commerceflow.reporting;

import com.sskengine.commerceflow.contracts.EventEnvelope;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


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
