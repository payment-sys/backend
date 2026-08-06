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
}
