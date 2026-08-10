package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record RefundRequest(@NotNull UUID paymentId, @NotNull Money amount) {}
