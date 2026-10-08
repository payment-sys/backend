package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.entrypoint.dto.req.OrderCreateReq;
import com.v_payment.pay.order.entrypoint.dto.req.OrderItemCreateReq;
import com.v_payment.pay.order.entrypoint.dto.res.OrderCreateRes;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxType;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryMessage;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryStatus;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.order.repository.QuantityChangeOutboxRepository;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceTest {
    @Autowired
    OrderService orderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    QuantityChangeOutboxRepository quantityChangeOutboxRepository;

    @DisplayName("주문을 생성하면 주문상품과 재고 차감 이벤트가 저장된다")
    @Test
    @Transactional
    void create() {
        // given
        Product productA = productRepository.save(Product.create("상품 A", 10_000L, 10));
        Product productB = productRepository.save(Product.create("상품 B", 5_000L, 10));

        OrderCreateReq req = new OrderCreateReq(
                PaymentMethod.CARD,
                List.of(
                        new OrderItemCreateReq(productA.getId(), 2),
                        new OrderItemCreateReq(productB.getId(), 3)
                )
        );

        // when
        OrderCreateRes res = orderService.create(req);

        // then
        Order order = orderRepository.findAll().get(0);

        assertThat(res.orderCode()).isEqualTo(order.getOrderCode());
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualTo(35_000L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        QuantityChangeOutbox outbox = quantityChangeOutboxRepository.findAll().get(0);
        assertThat(outbox.getOrderCode()).isEqualTo(order.getOrderCode());
        assertThat(outbox.getType()).isEqualTo(QuantityChangeOutboxType.DECREASE);
        assertThat(outbox.getStatus()).isEqualTo(QuantityChangeOutboxStatus.READY);
        assertThat(outbox.getReqQuantities().getQuantityMap())
                .containsEntry(productA.getId(), 2)
                .containsEntry(productB.getId(), 3);
        assertThat(outbox.getQuantityChangeMessages())
                .extracting("changeCount")
                .containsExactlyInAnyOrder(-2, -3);
    }

    @DisplayName("재고 차감 집계가 실패하면 주문 실패 결과를 저장하고 성공 차감 상품 보상 아웃박스를 만든다.")
    @Test
    @Transactional
    void finalizeOrderSummaryBatchWithCompensation() {
        // given
        Product productA = productRepository.save(Product.create("product-A", 10_000L, 10));
        Product productB = productRepository.save(Product.create("product-B", 5_000L, 10));
        OrderCreateRes res = orderService.create(new OrderCreateReq(
                PaymentMethod.CARD,
                List.of(
                        new OrderItemCreateReq(productA.getId(), 2),
                        new OrderItemCreateReq(productB.getId(), 3)
                )
        ));

        // when
        orderService.finalizeOrderSummaryBatch(List.of(new QuantityChangeSummaryMessage(
                res.orderCode(),
                QuantityChangeSummaryStatus.FAILED,
                List.of(productA.getId()),
                List.of(productB.getId())
        )));

        // then
        Order order = orderRepository.findByOrderCode(res.orderCode()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.LACK_QUANTITY);
        assertThat(order.getQuantityChangeSuccessProductIds()).containsExactly(productA.getId());
        assertThat(order.getQuantityChangeFailedProductIds()).containsExactly(productB.getId());

        List<QuantityChangeOutbox> outboxes = quantityChangeOutboxRepository.findAll();
        assertThat(outboxes).hasSize(2);
        QuantityChangeOutbox compensationOutbox = outboxes.stream()
                .filter(outbox -> outbox.getType() == QuantityChangeOutboxType.COMPENSATE)
                .findFirst()
                .orElseThrow();
        assertThat(compensationOutbox.getReqQuantities().getQuantityMap())
                .containsEntry(productA.getId(), 2)
                .doesNotContainKey(productB.getId());
        assertThat(compensationOutbox.getQuantityChangeMessages())
                .extracting("changeCount")
                .containsExactly(2);
    }

    @DisplayName("존재하지 않는 상품이 포함될 시 주문 생성에 실패한다.")
    @Test
    void createWithNotFoundProduct() {
        // given
        OrderCreateReq req = new OrderCreateReq(
                PaymentMethod.CARD,
                List.of(new OrderItemCreateReq(999L, 1))
        );

        // when & then
        assertThatThrownBy(() -> orderService.create(req)).isInstanceOf(BusinessException.class);

        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(quantityChangeOutboxRepository.findAll()).isEmpty();
    }
}
