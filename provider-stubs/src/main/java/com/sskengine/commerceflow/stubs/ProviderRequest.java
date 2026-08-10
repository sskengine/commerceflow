package com.sskengine.commerceflow.stubs;

import jakarta.validation.constraints.NotBlank;


record ProviderRequest(@NotBlank String operationId) {}
