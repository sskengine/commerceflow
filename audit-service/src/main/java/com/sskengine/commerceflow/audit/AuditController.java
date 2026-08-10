package com.sskengine.commerceflow.audit;

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
