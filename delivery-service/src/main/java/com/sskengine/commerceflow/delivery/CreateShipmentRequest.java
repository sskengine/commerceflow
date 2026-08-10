package com.sskengine.commerceflow.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


record CreateShipmentRequest(@NotNull UUID orderId, @NotBlank String addressRef) {}
