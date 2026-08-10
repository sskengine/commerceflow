package com.sskengine.commerceflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

record CreateOrderRequest(@NotNull UUID customerId, @NotNull @Size(min = 3, max = 3) String currency,
                          @NotEmpty List<@Valid OrderItemRequest> items) {}
