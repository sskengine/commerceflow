package com.sskengine.commerceflow.delivery;

import java.time.Instant;
import java.util.UUID;


class ShipmentEntity {
    private UUID shipmentId;
    private UUID orderId;
    private String addressRef;
    private String trackingNumber;
    private String status;
    private String providerReference;
    private String correlationId;
    private Instant createdAt;

    ShipmentEntity() {}

    ShipmentEntity(UUID shipmentId, UUID orderId, String addressRef, String trackingNumber, String status,
                   String providerReference, String correlationId, Instant createdAt) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.addressRef = addressRef;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.providerReference = providerReference;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID getShipmentId() { return shipmentId; }
    public void setShipmentId(UUID shipmentId) { this.shipmentId = shipmentId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getAddressRef() { return addressRef; }
    public void setAddressRef(String addressRef) { this.addressRef = addressRef; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    void updateStatus(String status) {
        this.status = status;
    }

    ShipmentView toView() {
        return new ShipmentView(shipmentId, orderId, addressRef, trackingNumber, status, createdAt);
    }
}
