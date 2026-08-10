package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


record PaymentRequest(@NotNull UUID orderId, @NotNull Money amount, @NotBlank String paymentMethodToken) {}
