package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Modifying
    @Query("""
    UPDATE Order o
    SET o.orderStatus = :paidStatus
    WHERE o.orderCode = :orderCode
    AND (o.orderStatus = :pendingPaymentStatus
        OR o.orderStatus = :paymentFailedStatus
        OR o.orderStatus = :paidStatus)
    """)
    int markPaid(@Param("orderCode") String orderCode,
                 @Param("pendingPaymentStatus") OrderStatus pendingPaymentStatus,
                 @Param("paymentFailedStatus") OrderStatus paymentFailedStatus,
                 @Param("paidStatus") OrderStatus paidStatus);

    @Modifying
    @Query("""
    UPDATE Order o
    SET o.orderStatus = :paymentFailedStatus
    WHERE o.orderCode = :orderCode
    AND (o.orderStatus = :pendingPaymentStatus OR o.orderStatus = :paymentFailedStatus)
    """)
    int markPaymentFailed(@Param("orderCode") String orderCode,
                          @Param("pendingPaymentStatus") OrderStatus pendingPaymentStatus,
                          @Param("paymentFailedStatus") OrderStatus paymentFailedStatus);
}
