package com.v_payment.pay.payment.exception;

import com.v_payment.pay.global.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentException implements ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 Payment를 찾지 못했습니다."),
    PAYMENT_INVALID(HttpStatus.CONFLICT, "해당 Payment가 유효하지 않습니다."),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
