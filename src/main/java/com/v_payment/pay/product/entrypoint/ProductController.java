package com.v_payment.pay.product.entrypoint;

import com.v_payment.pay.product.entrypoint.dto.req.ProductCreateReq;
import com.v_payment.pay.product.entrypoint.dto.res.ProductCreateRes;
import com.v_payment.pay.product.application.ProductUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductUseCase productUseCase;

    @PostMapping
    public ProductCreateRes createProduct(@Valid @RequestBody ProductCreateReq req) {
        return productUseCase.create(req);
    }
}
