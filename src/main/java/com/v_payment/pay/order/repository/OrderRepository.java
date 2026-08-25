package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Query("""
            update Order o
            set o.orderStatus = :nextStatus
            where o.orderCode = :orderCode
              and o.orderStatus = :currentStatus
            """)
    int updateStatus(
            @Param("orderCode") String orderCode,
            @Param("currentStatus") OrderStatus currentStatus,
            @Param("nextStatus") OrderStatus nextStatus
    );

    @Modifying
    @Query("""
            update Order o
            set o.orderStatus = :nextStatus
            where o.orderCode = :orderCode
              and o.orderStatus in :currentStatuses
            """)
    int updateStatus(
            @Param("orderCode") String orderCode,
            @Param("currentStatuses") Collection<OrderStatus> currentStatuses,
            @Param("nextStatus") OrderStatus nextStatus
    );

    @Modifying
    @Query("""
            update Order o
            set o.orderStatus = :nextStatus
            where o.orderCode in :orderCodes
              and o.orderStatus = :currentStatus
            """)
    int updateStatusByOrderCodes(
            @Param("orderCodes") Collection<String> orderCodes,
            @Param("currentStatus") OrderStatus currentStatus,
            @Param("nextStatus") OrderStatus nextStatus
    );
}
