package com.sskengine.commerceflow.audit;

import jakarta.validation.constraints.NotBlank;

record AuditCommand(@NotBlank String actorId, @NotBlank String action, @NotBlank String resourceType,
                    @NotBlank String resourceId, @NotBlank String correlationId) {}
