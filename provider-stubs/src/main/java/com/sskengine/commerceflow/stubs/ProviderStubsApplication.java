package com.sskengine.commerceflow.stubs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ProviderStubsApplication {
    public static void main(String[] args) { SpringApplication.run(ProviderStubsApplication.class, args); }
}

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

record ProviderRequest(@NotBlank String operationId) {}
record ProviderResponse(String operationId, String status, String providerReference) {}
@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "PROVIDER_FAILURE") class ProviderFailureException extends RuntimeException {}

