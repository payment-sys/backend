package com.v_payment.pay.product.service;

import com.v_payment.pay.product.controller.dto.req.ProductCreateReq;
import com.v_payment.pay.product.controller.dto.res.ProductCreateRes;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    ProductService productService;

    @Autowired
    ProductRepository productRepository;

    @DisplayName("상품을 생성하면 상품 정보가 저장되고 응답으로 반환된다")
    @Test
    void create() {
        // given
        ProductCreateReq req = new ProductCreateReq("product-A", 10_000L, 100);

        // when
        ProductCreateRes res = productService.create(req);

        // then
        Product product = productRepository.findById(res.productId()).orElseThrow();
        assertThat(res.name()).isEqualTo("product-A");
        assertThat(res.price()).isEqualTo(10_000L);
        assertThat(res.stockQuantity()).isEqualTo(100);
        assertThat(product.getName()).isEqualTo("product-A");
        assertThat(product.getPrice()).isEqualTo(10_000L);
        assertThat(product.getStockQuantity()).isEqualTo(100);
    }
}
