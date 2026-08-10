package com.sskengine.commerceflow.stubs;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "PROVIDER_FAILURE")
class ProviderFailureException extends RuntimeException {}
