package com.sskengine.commerceflow.catalog;

import com.sskengine.commerceflow.contracts.Money;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
