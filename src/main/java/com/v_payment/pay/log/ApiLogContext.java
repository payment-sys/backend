package com.v_payment.pay.log;

import jakarta.servlet.http.HttpServletRequest;

public record ApiLogContext(
    String method,
    String path
) {
    public static final String LOG_TYPE = "LOG_TYPE";
    public static final String METHOD = "METHOD";
    public static final String PATH = "PATH";

    public static ApiLogContext create(HttpServletRequest request) {
        return new ApiLogContext(
            request.getMethod(),
            request.getRequestURI()
        );
    }
}
