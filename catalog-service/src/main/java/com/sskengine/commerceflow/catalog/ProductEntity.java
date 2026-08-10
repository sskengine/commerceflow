package com.sskengine.commerceflow.catalog;

import com.sskengine.commerceflow.contracts.Money;
import java.math.BigDecimal;
import java.util.UUID;


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
