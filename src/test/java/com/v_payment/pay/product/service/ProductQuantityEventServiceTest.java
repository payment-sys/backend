package com.v_payment.pay.product.service;

import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.domain.entity.OrderStatus;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import com.v_payment.pay.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductQuantityEventServiceTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 10, 0, 0);

    @Autowired
    ProductQuantityEventService productQuantityEventService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductQuantityEventRepository productQuantityEventRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @DisplayName("Consuming a READY event decreases stock and creates a READY payment")
    @Test
    void consumeReadyEventWithSuccessEvent() {
        // given
        Product productA = saveProduct("product-A", 10_000L, 10);
        Product productB = saveProduct("product-B", 5_000L, 10);
        ProductQuantityEvent event = saveReadyEvent("ORDER-001", Map.of(
                productA.getId(), 2,
                productB.getId(), 3
        ));

        // when
        productQuantityEventService.consumeReadyEvent(10);

        // then
        assertThat(stockQuantityOf(productA)).isEqualTo(8);
        assertThat(stockQuantityOf(productB)).isEqualTo(7);
        assertThat(statusOf(event)).isEqualTo(ProductQuantityEventStatus.CONSUMED);

        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getOrderCode()).isEqualTo("ORDER-001");
        assertThat(payment.getRequestedAmount()).isEqualTo(35_000L);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
    }

    @DisplayName("A READY event with insufficient stock marks order as LACK_QUANTITY")
    @Test
    void consumeReadyEventWithFailureEvent() {
        // given
        Product product = saveProduct("product-A", 10_000L, 1);
        Order order = saveOrder("ORDER-001");
        ProductQuantityEvent event = saveReadyEvent("ORDER-001", Map.of(product.getId(), 2));

        // when
        productQuantityEventService.consumeReadyEvent(10);

        // then
        assertThat(stockQuantityOf(product)).isEqualTo(1);
        assertThat(statusOf(event)).isEqualTo(ProductQuantityEventStatus.CONSUMED);
        assertThat(orderRepository.findById(order.getOrderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.LACK_QUANTITY);
        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @DisplayName("Events for the same product are planned in event order")
    @Test
    void consumeReadyEventWithAccumulatedDecrease() {
        // given
        Product product = saveProduct("product-A", 10_000L, 4);
        Order failureOrder = saveOrder("ORDER-002");
        ProductQuantityEvent first = saveReadyEvent("ORDER-001", Map.of(product.getId(), 3));
        ProductQuantityEvent second = saveReadyEvent("ORDER-002", Map.of(product.getId(), 2));

        // when
        productQuantityEventService.consumeReadyEvent(10);

        // then
        assertThat(stockQuantityOf(product)).isEqualTo(1);
        assertThat(statusOf(first)).isEqualTo(ProductQuantityEventStatus.CONSUMED);
        assertThat(statusOf(second)).isEqualTo(ProductQuantityEventStatus.CONSUMED);
        assertThat(orderRepository.findById(failureOrder.getOrderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.LACK_QUANTITY);

        assertThat(paymentRepository.findAll())
                .extracting(Payment::getOrderCode)
                .containsExactly("ORDER-001");
    }

    @DisplayName("Only batchSize READY events are consumed")
    @Test
    void consumeReadyEventWithBatchSize() {
        // given
        Product product = saveProduct("product-A", 10_000L, 10);
        ProductQuantityEvent first = saveReadyEvent("ORDER-001", Map.of(product.getId(), 1));
        ProductQuantityEvent second = saveReadyEvent("ORDER-002", Map.of(product.getId(), 1));

        // when
        productQuantityEventService.consumeReadyEvent(1);

        // then
        assertThat(stockQuantityOf(product)).isEqualTo(9);
        assertThat(statusOf(first)).isEqualTo(ProductQuantityEventStatus.CONSUMED);
        assertThat(statusOf(second)).isEqualTo(ProductQuantityEventStatus.READY);
        assertThat(paymentRepository.findAll())
                .extracting(Payment::getOrderCode)
                .containsExactly("ORDER-001");
    }

    private Product saveProduct(String name, Long price, Integer stockQuantity) {
        return productRepository.save(Product.create(name, price, stockQuantity));
    }

    private Order saveOrder(String orderCode) {
        return orderRepository.save(Order.create(orderCode, CREATED_AT));
    }

    private ProductQuantityEvent saveReadyEvent(String orderCode, Map<Long, Integer> requestedQuantities) {
        return productQuantityEventRepository.save(new ProductQuantityEvent(
                orderCode,
                ProductQuantityEventPayload.of(orderCode, PaymentMethod.CARD, requestedQuantities),
                ProductQuantityEventStatus.READY,
                CREATED_AT,
                null
        ));
    }

    private Integer stockQuantityOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }

    private ProductQuantityEventStatus statusOf(ProductQuantityEvent event) {
        return productQuantityEventRepository.findById(event.getId())
                .orElseThrow()
                .getProductQuantityEventStatus();
    }
}
