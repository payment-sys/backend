package com.v_payment.pay.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getStatus();
    String getMessage();
}
