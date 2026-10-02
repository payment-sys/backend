package com.v_payment.pay.product.service;

import com.v_payment.pay.product.domain.ProductBasicInfo;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductManagerTest {

    @Autowired
    ProductManager productManager;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    EntityManager entityManager;

    @DisplayName("상품 재고를 복구하면 같은 상품의 수량을 합산해서 반영한다")
    @Test
    void restore() {
        Product product = productRepository.save(Product.create("product-A", 10_000L, 10));
        List<ProductManager.ProductRestoreReq> requests = List.of(
                new ProductManager.ProductRestoreReq(product.getId(), 2),
                new ProductManager.ProductRestoreReq(product.getId(), 3)
        );

        productManager.restore(requests);
        entityManager.clear();

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getStockQuantity()).isEqualTo(15);
    }

    @DisplayName("존재하지 않는 상품 재고 복구는 실패한다")
    @Test
    void restoreWithNotFoundProduct() {
        List<ProductManager.ProductRestoreReq> requests = List.of(
                new ProductManager.ProductRestoreReq(999L, 1)
        );

        assertThatThrownBy(() -> productManager.restore(requests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("상품 재고 복구에 실패했습니다. productId = 999");
    }

    @DisplayName("상품 기본 정보를 조회한다")
    @Test
    void findProductBasicInfos() {
        Product productA = productRepository.save(Product.create("product-A", 10_000L, 10));
        Product productB = productRepository.save(Product.create("product-B", 20_000L, 20));

        List<ProductBasicInfo> productBasicInfos = productManager.findProductBasicInfos(
                List.of(productA.getId(), productB.getId())
        );

        assertThat(productBasicInfos)
                .extracting(ProductBasicInfo::productId)
                .containsExactlyInAnyOrder(productA.getId(), productB.getId());
    }

    @DisplayName("상품 목록을 락 조회하고 ID 기준 Map으로 변환한다")
    @Test
    void findProductsMapForUpdate() {
        Product productA = productRepository.save(Product.create("product-A", 10_000L, 10));
        Product productB = productRepository.save(Product.create("product-B", 20_000L, 20));

        Map<Long, Product> products = productManager.findProductsMapForUpdate(
                List.of(productA.getId(), productB.getId())
        );

        assertThat(products).containsOnlyKeys(productA.getId(), productB.getId());
        assertThat(products.get(productA.getId()).getName()).isEqualTo("product-A");
        assertThat(products.get(productB.getId()).getName()).isEqualTo("product-B");
    }
}
