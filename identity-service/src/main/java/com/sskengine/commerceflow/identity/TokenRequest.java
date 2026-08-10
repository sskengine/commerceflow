package com.sskengine.commerceflow.identity;

import jakarta.validation.constraints.NotBlank;

record TokenRequest(@NotBlank String username, @NotBlank String password) {}
