package com.sskengine.commerceflow.contracts;

import java.time.Instant;

/** INT-002: stable machine code, safe message and correlation ID. */
public record ApiError(String code, String message, String correlationId, Instant timestamp) {
    public static ApiError of(String code, String message, String correlationId) {
        return new ApiError(code, message, correlationId, Instant.now());
    }
}

