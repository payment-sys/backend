package com.v_payment.pay.product.service;

import com.v_payment.pay.product.controller.dto.req.ProductCreateReq;
import com.v_payment.pay.product.controller.dto.res.ProductCreateRes;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional
    public ProductCreateRes create(ProductCreateReq req) {
        Product product = productRepository.save(
                Product.create(req.name(), req.price(), req.stockQuantity())
        );
        return ProductCreateRes.from(product);
    }
}
