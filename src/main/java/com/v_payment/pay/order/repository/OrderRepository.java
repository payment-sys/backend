package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.domain.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);

    @Modifying
    @Query("""
            update Order o
            set o.status = :failedStatus
            where o.orderCode = :orderCode
              and o.status = :createdStatus
            """)
    int markFailed(
            @Param("orderCode") String orderCode,
            @Param("createdStatus") OrderStatus createdStatus,
            @Param("failedStatus") OrderStatus failedStatus
    );

    @Modifying
    @Query("""
            update Order o
            set o.status = :failedStatus
            where o.orderCode in :orderCodes
              and o.status = :createdStatus
            """)
    int markFailedByOrderCodes(
            @Param("orderCodes") Collection<String> orderCodes,
            @Param("createdStatus") OrderStatus createdStatus,
            @Param("failedStatus") OrderStatus failedStatus
    );
}
