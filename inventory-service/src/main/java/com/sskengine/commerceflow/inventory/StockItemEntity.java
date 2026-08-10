package com.sskengine.commerceflow.inventory;

import java.util.UUID;


class StockItemEntity {
    private UUID productId;
    private int availableQty;

    StockItemEntity() {}

    StockItemEntity(UUID productId, int availableQty) {
        this.productId = productId;
        this.availableQty = availableQty;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }

    void decrease(int quantity) {
        if (availableQty < quantity) {
            throw new InsufficientStockException();
        }
        availableQty -= quantity;
    }

    void increase(int quantity) {
        availableQty += quantity;
    }
}
