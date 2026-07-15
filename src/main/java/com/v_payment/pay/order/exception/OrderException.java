package com.v_payment.pay.order.exception;

import com.v_payment.pay.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderException implements ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
