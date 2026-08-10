package com.sskengine.commerceflow.catalog;

import com.sskengine.commerceflow.contracts.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}

@Entity
@Table(name = "product")
class ProductEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false)
    private boolean published;

    protected ProductEntity() {}

    ProductEntity(UUID id, String name, Money price, boolean published) {
        this.id = id;
        this.name = name;
        this.amount = price.amount();
        this.currency = price.currency();
        this.published = published;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    Money price() { return new Money(amount, currency); }
    boolean isPublished() { return published; }

    ProductView toView() {
        return new ProductView(id, name, price(), published);
    }
}

@Repository
interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    List<ProductEntity> findByPublishedTrueAndNameContainingIgnoreCase(String name);
}

/** FR-010..012: catalog browse and publish. */
@Service
class CatalogService {
    private final ProductRepository products;

    CatalogService(ProductRepository products) {
        this.products = products;
    }

    List<ProductView> search(String q) {
        return products.findByPublishedTrueAndNameContainingIgnoreCase(q == null ? "" : q)
                .stream().map(ProductEntity::toView).toList();
    }

    ProductView get(UUID id) {
        return products.findById(id).map(ProductEntity::toView).orElseThrow(ProductNotFoundException::new);
    }

    @Transactional
    ProductView create(CreateProductRequest request) {
        ProductEntity entity = new ProductEntity(UUID.randomUUID(), request.name(),
                new Money(request.amount(), request.currency()), true);
        return products.save(entity).toView();
    }
}

@RestController
@RequestMapping("/api/v1/products")
class CatalogController {
    private final CatalogService service;

    CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    List<ProductView> search(@RequestParam(defaultValue = "") String q) {
        return service.search(q);
    }

    @GetMapping("/{id}")
    ProductView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductView create(@Valid @RequestBody CreateProductRequest request) {
        return service.create(request);
    }
}

record CreateProductRequest(@NotBlank @Size(max = 200) String name,
                            @NotNull BigDecimal amount,
                            @NotBlank @Size(min = 3, max = 3) String currency) {}

record ProductView(UUID id, String name, Money price, boolean published) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProductNotFoundException extends RuntimeException {}
