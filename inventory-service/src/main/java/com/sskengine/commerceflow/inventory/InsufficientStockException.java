package com.sskengine.commerceflow.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(code = HttpStatus.CONFLICT, reason = "INSUFFICIENT_STOCK")
class InsufficientStockException extends RuntimeException {}
