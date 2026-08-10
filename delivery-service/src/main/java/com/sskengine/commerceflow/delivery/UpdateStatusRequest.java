package com.sskengine.commerceflow.delivery;

import jakarta.validation.constraints.NotBlank;

record UpdateStatusRequest(@NotBlank String status) {}
