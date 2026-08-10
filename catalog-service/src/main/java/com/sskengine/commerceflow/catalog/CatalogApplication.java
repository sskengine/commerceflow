package com.sskengine.commerceflow.catalog;

import com.sskengine.commerceflow.contracts.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
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

class ProductEntity {
    private UUID id;
    private String name;
    private BigDecimal amount;
    private String currency;
    private boolean published;

    ProductEntity() {}

    ProductEntity(UUID id, String name, Money price, boolean published) {
        this.id = id;
        this.name = name;
        this.amount = price.amount();
        this.currency = price.currency();
        this.published = published;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    Money price() { return new Money(amount, currency); }

    ProductView toView() {
        return new ProductView(id, name, price(), published);
    }
}

@Mapper
interface ProductRepository {
    @Select("""
            SELECT id, name, amount, currency, published
            FROM product
            WHERE published = TRUE AND LOWER(name) LIKE LOWER(CONCAT('%', #{name}, '%'))
            """)
    List<ProductEntity> findByPublishedTrueAndNameContainingIgnoreCase(@Param("name") String name);

    @Select("SELECT id, name, amount, currency, published FROM product WHERE id = #{id}")
    ProductEntity findById(@Param("id") UUID id);

    @Insert("""
            INSERT INTO product (id, name, amount, currency, published)
            VALUES (#{id}, #{name}, #{amount}, #{currency}, #{published})
            """)
    void insert(ProductEntity entity);
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
        ProductEntity entity = products.findById(id);
        if (entity == null) {
            throw new ProductNotFoundException();
        }
        return entity.toView();
    }

    @Transactional
    ProductView create(CreateProductRequest request) {
        ProductEntity entity = new ProductEntity(UUID.randomUUID(), request.name(),
                new Money(request.amount(), request.currency()), true);
        products.insert(entity);
        return entity.toView();
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
