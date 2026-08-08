package com.sskengine.commerceflow.order;

import com.sskengine.commerceflow.contracts.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) { SpringApplication.run(OrderApplication.class, args); }
    @Bean RestClient inventoryClient(@Value("${clients.inventory.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}

/** FR-021, FR-022, DATA-002, BRULE-001, BRULE-003, AC-001. */
@Service
class OrderService {
    private final RestClient inventory;
    private final Map<UUID, OrderView> orders = new ConcurrentHashMap<>();
    OrderService(RestClient inventoryClient) { this.inventory = inventoryClient; }

    OrderView create(CreateOrderRequest request, String correlationId) {
        UUID orderId = UUID.randomUUID();
        Money total = request.items().stream()
                .map(item -> new Money(item.unitPrice(), request.currency()).multiply(item.quantity()))
                .reduce(new Money(BigDecimal.ZERO, request.currency()),
                        (a, b) -> new Money(a.amount().add(b.amount()), a.currency()));
        for (OrderItemRequest item : request.items()) {
            inventory.post().uri("/api/v1/reservations")
                    .header("X-Correlation-Id", correlationId)
                    .body(new ReserveStockRequest(orderId, item.productId(), item.quantity()))
                    .retrieve().toBodilessEntity();
        }
        OrderView order = new OrderView(orderId, request.customerId(), request.currency(), total,
                List.copyOf(request.items()), "CONFIRMED", Instant.now(), correlationId);
        orders.put(orderId, order);
        return order;
    }

    OrderView get(UUID id) {
        OrderView order = orders.get(id);
        if (order == null) throw new OrderNotFoundException();
        return order;
    }
}

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {
    private final OrderService service;
    OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderView create(@Valid @RequestBody CreateOrderRequest request,
                     @RequestHeader(name = "X-Correlation-Id") @Size(min = 8, max = 128) String correlationId) {
        return service.create(request, correlationId);
    }

    @GetMapping("/{id}") OrderView get(@PathVariable UUID id) { return service.get(id); }
}

record CreateOrderRequest(@NotNull UUID customerId, @NotNull @Size(min = 3, max = 3) String currency,
                          @NotEmpty List<@Valid OrderItemRequest> items) {}
record OrderItemRequest(@NotNull UUID productId, @Positive int quantity, @NotNull BigDecimal unitPrice) {}
record ReserveStockRequest(UUID orderId, UUID productId, int quantity) {}
record OrderView(UUID orderId, UUID customerId, String currency, Money total, List<OrderItemRequest> items,
                 String status, Instant createdAt, String correlationId) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class OrderNotFoundException extends RuntimeException {}

