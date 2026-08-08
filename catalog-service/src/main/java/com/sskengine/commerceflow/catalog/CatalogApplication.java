package com.sskengine.commerceflow.catalog;

import com.sskengine.commerceflow.contracts.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) { SpringApplication.run(CatalogApplication.class, args); }
}

/** FR-010..012: v0.1 read-only catalog baseline. */
@RestController
@RequestMapping("/api/v1/products")
class CatalogController {
    static final UUID DEMO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final List<Product> products = List.of(
            new Product(DEMO_ID, "Demo keyboard", new Money(new BigDecimal("25.00"), "USD"), true),
            new Product(UUID.fromString("33333333-3333-3333-3333-333333333333"), "Demo mouse", new Money(new BigDecimal("15.00"), "USD"), true));

    @GetMapping
    List<Product> search(@RequestParam(defaultValue = "") String q) {
        return products.stream().filter(p -> p.published() && p.name().toLowerCase().contains(q.toLowerCase())).toList();
    }

    @GetMapping("/{id}")
    Product get(@PathVariable UUID id) {
        return products.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow(ProductNotFoundException::new);
    }

    record Product(UUID id, String name, Money price, boolean published) {}
}

@org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
class ProductNotFoundException extends RuntimeException {}

