package com.sskengine.commerceflow.gateway;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) { SpringApplication.run(ApiGatewayApplication.class, args); }
    @Bean RestClient catalogClient(@Value("${clients.catalog.base-url}") String url) { return RestClient.create(url); }
    @Bean RestClient orderClient(@Value("${clients.order.base-url}") String url) { return RestClient.create(url); }
}

/** INT-001, INT-002: public BFF keeps service locations out of client contracts. */
@RestController
@RequestMapping("/api")
class GatewayController {
    private final RestClient catalog;
    private final RestClient order;
    GatewayController(RestClient catalogClient, RestClient orderClient) {
        this.catalog = catalogClient;
        this.order = orderClient;
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
}

