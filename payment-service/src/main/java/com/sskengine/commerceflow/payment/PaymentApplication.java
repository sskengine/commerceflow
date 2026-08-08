package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PaymentApplication {
    public static void main(String[] args) { SpringApplication.run(PaymentApplication.class, args); }
}

/** FR-040, FR-041, INT-003, SEC-004, AC-002, AC-004. */
@Service
class PaymentService {
    private final Map<String, PaymentView> byIdempotencyKey = new ConcurrentHashMap<>();

    PaymentView authorize(PaymentRequest request, String idempotencyKey, String correlationId) {
        return byIdempotencyKey.computeIfAbsent(idempotencyKey, ignored ->
                new PaymentView(UUID.randomUUID(), request.orderId(), request.amount(), "AUTHORIZED",
                        Instant.now(), correlationId));
    }
}

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {
    private final PaymentService service;
    PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentView authorize(@Valid @RequestBody PaymentRequest request,
                          @RequestHeader(name = "Idempotency-Key") @NotBlank String idempotencyKey,
                          @RequestHeader(name = "X-Correlation-Id") @NotBlank String correlationId) {
        return service.authorize(request, idempotencyKey, correlationId);
    }
}

record PaymentRequest(@NotNull UUID orderId, @NotNull Money amount, @NotBlank String paymentMethodToken) {}
record PaymentView(UUID paymentId, UUID orderId, Money amount, String status, Instant createdAt, String correlationId) {}

