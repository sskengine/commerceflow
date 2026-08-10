package com.sskengine.commerceflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/orders")
class OrderController {
    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderView create(@Valid @RequestBody CreateOrderRequest request,
                     @RequestHeader(name = "X-Correlation-Id") @Size(min = 8, max = 128) String correlationId) {
        return service.create(request, correlationId);
    }

    @GetMapping("/{id}")
    OrderView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    OrderView cancel(@PathVariable UUID id,
                     @RequestHeader(name = "X-Correlation-Id") @Size(min = 8, max = 128) String correlationId) {
        return service.cancel(id, correlationId);
    }
}
