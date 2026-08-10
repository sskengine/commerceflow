package com.sskengine.commerceflow.contracts;

/** INT-004..005: versioned topic names matching contracts/asyncapi. */
public final class CommerceTopics {
    public static final String ORDER_CREATED = "commerceflow.order-created.v1";
    public static final String ORDER_STATUS_CHANGED = "commerceflow.order-status-changed.v1";
    public static final String STOCK_RESERVED = "commerceflow.stock-reserved.v1";
    public static final String PAYMENT_AUTHORIZED = "commerceflow.payment-authorized.v1";
    public static final String PAYMENT_FAILED = "commerceflow.payment-failed.v1";
    public static final String REFUND_COMPLETED = "commerceflow.refund-completed.v1";
    public static final String SHIPMENT_CREATED = "commerceflow.shipment-created.v1";

    private CommerceTopics() {}
}
