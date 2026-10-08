package com.v_payment.pay.order.entrypoint;

import com.v_payment.pay.order.entrypoint.dto.req.OrderCreateReq;
import com.v_payment.pay.order.entrypoint.dto.res.OrderCreateRes;
import com.v_payment.pay.order.application.OrderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderUseCase orderUsecase;

    @PostMapping
    public OrderCreateRes create(@Valid @RequestBody OrderCreateReq req) {
        return orderUsecase.create(req);
    }
}
