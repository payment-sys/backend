package com.v_payment.pay.product.application;

import com.v_payment.pay.product.entrypoint.dto.req.ProductCreateReq;
import com.v_payment.pay.product.entrypoint.dto.res.ProductCreateRes;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductUseCase {
    private final ProductRepository productRepository;

    @Transactional("productTransactionManager")
    public ProductCreateRes create(ProductCreateReq req) {
        Product product = Product.create(req.name(), req.price(), req.stockQuantity());
        productRepository.save(product);
        return ProductCreateRes.from(product);
    }
}
