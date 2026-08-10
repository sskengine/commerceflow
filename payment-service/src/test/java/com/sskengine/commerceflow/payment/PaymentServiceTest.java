package com.sskengine.commerceflow.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.sskengine.commerceflow.contracts.Money;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class PaymentServiceTest {
    static final MockWebServer provider = new MockWebServer();

    static {
        try {
            provider.start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Autowired
    PaymentService service;

    @MockBean
    DomainEventPublisher events;

    @AfterAll
    static void stopProvider() throws Exception {
        provider.shutdown();
    }

    @DynamicPropertySource
    static void registerProvider(DynamicPropertyRegistry registry) {
        registry.add("clients.provider.base-url", () -> provider.url("/").toString().replaceAll("/$", ""));
    }

    @Test
    void repeatedKeyReturnsOriginalPayment() {
        provider.enqueue(new MockResponse()
                .setBody("{\"operationId\":\"x\",\"status\":\"SUCCESS\",\"providerReference\":\"stub-1\"}")
                .addHeader("Content-Type", "application/json"));
        PaymentRequest request = new PaymentRequest(UUID.randomUUID(), new Money(new BigDecimal("25"), "USD"), "tok_demo");
        PaymentView first = service.authorize(request, "same-key", "corr-12345678");
        PaymentView second = service.authorize(request, "same-key", "corr-12345678");
        assertEquals(first.paymentId(), second.paymentId());
    }
}
