package com.sskengine.commerceflow.audit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
public class AuditApplication {
    public static void main(String[] args) { SpringApplication.run(AuditApplication.class, args); }
}

/** FR-090, SEC-005: append-only privileged operation history. */
@RestController
@RequestMapping("/api/v1/audit-events")
class AuditController {
    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    AuditEntry append(@Valid @RequestBody AuditCommand command) {
        AuditEntry entry = new AuditEntry(UUID.randomUUID(), command.actorId(), command.action(),
                command.resourceType(), command.resourceId(), command.correlationId(), Instant.now());
        entries.add(entry);
        return entry;
    }

    @GetMapping List<AuditEntry> list() { return List.copyOf(entries); }
}

record AuditCommand(@NotBlank String actorId, @NotBlank String action, @NotBlank String resourceType,
                    @NotBlank String resourceId, @NotBlank String correlationId) {}
record AuditEntry(UUID eventId, String actorId, String action, String resourceType,
                  String resourceId, String correlationId, Instant occurredAt) {}

