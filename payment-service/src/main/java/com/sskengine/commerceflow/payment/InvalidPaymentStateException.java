package com.sskengine.commerceflow.payment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.CONFLICT)
class InvalidPaymentStateException extends RuntimeException {}
