package com.sskengine.commerceflow.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/** INT-001, INT-002: public BFF keeps service locations out of client contracts. */
@RestController
@RequestMapping("/api")
class GatewayController {
    private final RestClient catalog;
    private final RestClient order;
    private final RestClient payment;

    GatewayController(RestClient catalogClient, RestClient orderClient, RestClient paymentClient) {
        this.catalog = catalogClient;
        this.order = orderClient;
        this.payment = paymentClient;
    }

    @GetMapping("/catalog/products")
    List<?> products() {
        List<?> body = catalog.get().uri("/api/v1/products").retrieve().body(List.class);
        return body == null ? List.of() : body;
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<?, ?> createOrder(@RequestBody Map<String, Object> request,
                          @RequestHeader("X-Correlation-Id") String correlationId) {
        Map<?, ?> body = order.post().uri("/api/v1/orders")
                .header("X-Correlation-Id", correlationId)
                .body(request).retrieve().body(Map.class);
        return body == null ? Map.of() : body;
    }

    @PostMapping(value = "/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<?, ?> authorizePayment(@RequestBody Map<String, Object> request,
                               @RequestHeader("X-Correlation-Id") String correlationId,
                               @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Map<?, ?> body = payment.post().uri("/api/v1/payments")
                .header("X-Correlation-Id", correlationId)
                .header("Idempotency-Key", idempotencyKey)
                .body(request).retrieve().body(Map.class);
        return body == null ? Map.of() : body;
    }
}
