package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** FR-040, FR-041, INT-003, SEC-004, AC-002, AC-004. Token is never logged. */
@Service
class PaymentService {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final RestClient provider;
    private final DomainEventPublisher events;

    PaymentService(PaymentRepository payments, RefundRepository refunds, RestClient providerClient,
                   DomainEventPublisher events) {
        this.payments = payments;
        this.refunds = refunds;
        this.provider = providerClient;
        this.events = events;
    }

    @Transactional
    PaymentView authorize(PaymentRequest request, String idempotencyKey, String correlationId) {
        PaymentEntity existing = payments.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing.toView();
        }

        UUID paymentId = UUID.randomUUID();
        String providerReference;
        String status;
        try {
            Map<?, ?> response = provider.post().uri("/stubs/v1/payments/authorize")
                    .header("X-Correlation-Id", correlationId)
                    .body(Map.of("operationId", paymentId.toString()))
                    .retrieve().body(Map.class);
            providerReference = response == null ? null : String.valueOf(response.get("providerReference"));
            status = "AUTHORIZED";
        } catch (RestClientResponseException ex) {
            PaymentEntity failed = new PaymentEntity(paymentId, request.orderId(), request.amount(), "FAILED",
                    idempotencyKey, null, correlationId, Instant.now());
            payments.insert(failed);
            events.publish(CommerceTopics.PAYMENT_FAILED, EventEnvelope.v1(
                    "PaymentFailed", correlationId,
                    new CommerceEvents.PaymentFailed(request.orderId(), "PROVIDER_FAILURE")));
            return failed.toView();
        }

        PaymentEntity payment = new PaymentEntity(paymentId, request.orderId(), request.amount(), status,
                idempotencyKey, providerReference, correlationId, Instant.now());
        payments.insert(payment);
        events.publish(CommerceTopics.PAYMENT_AUTHORIZED, EventEnvelope.v1(
                "PaymentAuthorized", correlationId,
                new CommerceEvents.PaymentAuthorized(paymentId, request.orderId(), request.amount())));
        return payment.toView();
    }

    @Transactional
    RefundView refund(RefundRequest request, String idempotencyKey, String correlationId) {
        RefundEntity existing = refunds.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing.toView();
        }
        PaymentEntity payment = payments.findById(request.paymentId());
        if (payment == null) {
            throw new PaymentNotFoundException();
        }
        if (!"AUTHORIZED".equals(payment.getStatus())) {
            throw new InvalidPaymentStateException();
        }
        RefundEntity refund = new RefundEntity(UUID.randomUUID(), payment.getPaymentId(), request.amount(),
                "COMPLETED", idempotencyKey, correlationId, Instant.now());
        refunds.insert(refund);
        events.publish(CommerceTopics.REFUND_COMPLETED, EventEnvelope.v1(
                "RefundCompleted", correlationId,
                new CommerceEvents.RefundCompleted(refund.toView().refundId(), payment.getPaymentId(), request.amount())));
        return refund.toView();
    }
}
