package com.sskengine.commerceflow.stubs;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** NFR-007: deterministic success/failure/timeout behavior for external dependencies. */
@RestController
@RequestMapping("/stubs/v1")
class ProviderController {
    @PostMapping("/payments/authorize")
    ProviderResponse payment(@Valid @RequestBody ProviderRequest request,
                             @RequestHeader(name = "X-Stub-Scenario", defaultValue = "success") String scenario)
            throws InterruptedException {
        return execute(request.operationId(), scenario);
    }

    @PostMapping("/logistics/shipments")
    ProviderResponse shipment(@Valid @RequestBody ProviderRequest request,
                              @RequestHeader(name = "X-Stub-Scenario", defaultValue = "success") String scenario)
            throws InterruptedException {
        return execute(request.operationId(), scenario);
    }

    private ProviderResponse execute(String operationId, String scenario) throws InterruptedException {
        if ("timeout".equalsIgnoreCase(scenario)) Thread.sleep(Duration.ofSeconds(10));
        if ("failure".equalsIgnoreCase(scenario)) throw new ProviderFailureException();
        return new ProviderResponse(operationId, "SUCCESS", "stub-" + UUID.randomUUID());
    }
}
