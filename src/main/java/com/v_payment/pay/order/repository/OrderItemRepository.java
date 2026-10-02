package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.domain.orderitem.OrderItem;
import com.v_payment.pay.order.domain.orderitem.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemJdbcRepository {

    @Query("""
            select oi
            from OrderItem oi
            where oi.order.orderCode = :orderCode
            """)
    List<OrderItem> findAllByOrderCode(@Param("orderCode") String orderCode);

    @Query("""
            select distinct oi.order.orderCode
            from OrderItem oi
            where oi.order.orderCode in :orderCodes
              and oi.status = :status
            """)
    List<String> findOrderCodesByItemStatus(
            @Param("orderCodes") Collection<String> orderCodes,
            @Param("status") OrderItemStatus status
    );
}
