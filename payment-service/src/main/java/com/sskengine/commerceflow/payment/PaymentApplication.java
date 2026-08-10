package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.contracts.Money;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@SpringBootApplication(scanBasePackages = {
        "com.sskengine.commerceflow.payment",
        "com.sskengine.commerceflow.messaging"
})
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }

    @Bean
    RestClient providerClient(@Value("${clients.provider.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}

class PaymentEntity {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String idempotencyKey;
    private String providerReference;
    private String correlationId;
    private Instant createdAt;

    PaymentEntity() {}

    PaymentEntity(UUID paymentId, UUID orderId, Money money, String status, String idempotencyKey,
                  String providerReference, String correlationId, Instant createdAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = money.amount();
        this.currency = money.currency();
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.providerReference = providerReference;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    Money money() { return new Money(amount, currency); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    PaymentView toView() {
        return new PaymentView(paymentId, orderId, money(), status, createdAt, correlationId);
    }
}

class RefundEntity {
    private UUID refundId;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String idempotencyKey;
    private String correlationId;
    private Instant createdAt;

    RefundEntity() {}

    RefundEntity(UUID refundId, UUID paymentId, Money money, String status, String idempotencyKey,
                 String correlationId, Instant createdAt) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.amount = money.amount();
        this.currency = money.currency();
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID getRefundId() { return refundId; }
    public void setRefundId(UUID refundId) { this.refundId = refundId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    RefundView toView() {
        return new RefundView(refundId, paymentId, new Money(amount, currency), status, createdAt, correlationId);
    }
}

@Mapper
interface PaymentRepository {
    @Select("""
            SELECT payment_id, order_id, amount, currency, status, idempotency_key, provider_reference,
                   correlation_id, created_at
            FROM payment
            WHERE idempotency_key = #{idempotencyKey}
            """)
    PaymentEntity findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT payment_id, order_id, amount, currency, status, idempotency_key, provider_reference,
                   correlation_id, created_at
            FROM payment
            WHERE payment_id = #{paymentId}
            """)
    PaymentEntity findById(@Param("paymentId") UUID paymentId);

    @Insert("""
            INSERT INTO payment (payment_id, order_id, amount, currency, status, idempotency_key,
                                 provider_reference, correlation_id, created_at)
            VALUES (#{paymentId}, #{orderId}, #{amount}, #{currency}, #{status}, #{idempotencyKey},
                    #{providerReference}, #{correlationId}, #{createdAt})
            """)
    void insert(PaymentEntity payment);
}

@Mapper
interface RefundRepository {
    @Select("""
            SELECT refund_id, payment_id, amount, currency, status, idempotency_key, correlation_id, created_at
            FROM refund
            WHERE idempotency_key = #{idempotencyKey}
            """)
    RefundEntity findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Insert("""
            INSERT INTO refund (refund_id, payment_id, amount, currency, status, idempotency_key,
                                correlation_id, created_at)
            VALUES (#{refundId}, #{paymentId}, #{amount}, #{currency}, #{status}, #{idempotencyKey},
                    #{correlationId}, #{createdAt})
            """)
    void insert(RefundEntity refund);
}

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

record PaymentRequest(@NotNull UUID orderId, @NotNull Money amount, @NotBlank String paymentMethodToken) {}
record RefundRequest(@NotNull UUID paymentId, @NotNull Money amount) {}
record PaymentView(UUID paymentId, UUID orderId, Money amount, String status, Instant createdAt, String correlationId) {}
record RefundView(UUID refundId, UUID paymentId, Money amount, String status, Instant createdAt, String correlationId) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class PaymentNotFoundException extends RuntimeException {}

@ResponseStatus(HttpStatus.CONFLICT)
class InvalidPaymentStateException extends RuntimeException {}
