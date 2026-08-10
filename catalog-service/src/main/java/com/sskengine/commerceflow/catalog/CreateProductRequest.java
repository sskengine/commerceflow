package com.sskengine.commerceflow.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

record CreateProductRequest(@NotBlank @Size(max = 200) String name,
                            @NotNull BigDecimal amount,
                            @NotBlank @Size(min = 3, max = 3) String currency) {}
