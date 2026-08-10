package com.sskengine.commerceflow.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {
    private final PaymentService service;

    PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentView authorize(@Valid @RequestBody PaymentRequest request,
                          @RequestHeader(name = "Idempotency-Key") @NotBlank String idempotencyKey,
                          @RequestHeader(name = "X-Correlation-Id") @NotBlank String correlationId) {
        return service.authorize(request, idempotencyKey, correlationId);
    }

    @PostMapping("/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    RefundView refund(@Valid @RequestBody RefundRequest request,
                      @RequestHeader(name = "Idempotency-Key") @NotBlank String idempotencyKey,
                      @RequestHeader(name = "X-Correlation-Id") @NotBlank String correlationId) {
        return service.refund(request, idempotencyKey, correlationId);
    }
}
