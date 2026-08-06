package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select oi
            from OrderItem oi
            where oi.order.orderCode = :orderCode
            """)
    List<OrderItem> findAllByOrderCode(@Param("orderCode") String orderCode);
}
