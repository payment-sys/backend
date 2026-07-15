package com.v_payment.pay.product.controller;

import com.v_payment.pay.product.controller.dto.req.ProductCreateReq;
import com.v_payment.pay.product.controller.dto.res.ProductCreateRes;
import com.v_payment.pay.product.service.ProductService;
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
    private final ProductService productService;

    @PostMapping
    public ProductCreateRes createProduct(@Valid @RequestBody ProductCreateReq req) {
        return productService.create(req);
    }
}
