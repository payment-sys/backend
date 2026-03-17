package com.v_payment.pay.global;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getStatus();
    String getMessage();
}
