package com.v_payment.pay.product.service;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.product.domain.ProductBasicInfo;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
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
    ProductQuantityEventRepository productQuantityEventRepository;

    @Autowired
    EntityManager entityManager;

    @DisplayName("상품 수량 이벤트를 생성하면 READY 상태로 저장된다")
    @Test
    void createProductQuantityEvent() {
        // given
        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(
                "ORDER-001",
                PaymentMethod.CARD,
                Map.of(1L, 2)
        );

        // when
        productManager.createProductQuantityEvent("ORDER-001", payload);

        // then
        ProductQuantityEvent event = productQuantityEventRepository.findAll().get(0);
        assertThat(event.getOrderCode()).isEqualTo("ORDER-001");
        assertThat(event.getPayload().getRequestedQuantities()).containsEntry(1L, 2);
        assertThat(event.getProductQuantityEventStatus()).isEqualTo(ProductQuantityEventStatus.READY);
    }

    @DisplayName("상품 재고를 복구하면 같은 상품의 수량을 합산해서 반영한다")
    @Test
    void restore() {
        // given
        Product product = productRepository.save(Product.create("product-A", 10_000L, 10));
        List<ProductManager.ProductRestoreReq> requests = List.of(
                new ProductManager.ProductRestoreReq(product.getId(), 2),
                new ProductManager.ProductRestoreReq(product.getId(), 3)
        );

        // when
        productManager.restore(requests);
        entityManager.clear();

        // then
        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getStockQuantity()).isEqualTo(15);
    }

    @DisplayName("존재하지 않는 상품 재고 복구는 실패한다")
    @Test
    void restoreWithNotFoundProduct() {
        // given
        List<ProductManager.ProductRestoreReq> requests = List.of(
                new ProductManager.ProductRestoreReq(999L, 1)
        );

        // when & then
        assertThatThrownBy(() -> productManager.restore(requests))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Product stock restore failed. productId = 999");
    }

    @DisplayName("상품 기본 정보를 조회한다")
    @Test
    void findProductBasicInfos() {
        // given
        Product productA = productRepository.save(Product.create("product-A", 10_000L, 10));
        Product productB = productRepository.save(Product.create("product-B", 20_000L, 20));

        // when
        List<ProductBasicInfo> productBasicInfos = productManager.findProductBasicInfos(
                List.of(productA.getId(), productB.getId())
        );

        // then
        assertThat(productBasicInfos)
                .extracting(ProductBasicInfo::productId)
                .containsExactlyInAnyOrder(productA.getId(), productB.getId());
    }

    @DisplayName("상품 목록을 락 조회하고 ID 기준 Map으로 변환한다")
    @Test
    void findProductsMapForUpdate() {
        // given
        Product productA = productRepository.save(Product.create("product-A", 10_000L, 10));
        Product productB = productRepository.save(Product.create("product-B", 20_000L, 20));

        // when
        Map<Long, Product> products = productManager.findProductsMapForUpdate(
                List.of(productA.getId(), productB.getId())
        );

        // then
        assertThat(products).containsOnlyKeys(productA.getId(), productB.getId());
        assertThat(products.get(productA.getId()).getName()).isEqualTo("product-A");
        assertThat(products.get(productB.getId()).getName()).isEqualTo("product-B");
    }
}
