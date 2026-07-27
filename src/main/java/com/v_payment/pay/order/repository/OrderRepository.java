package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT DISTINCT o
    FROM Order o
    LEFT JOIN FETCH o.orderItems
    WHERE o.orderCode = :orderCode
    """)
    Optional<Order> findByOrderCodeWithItemsForUpdate(@Param("orderCode") String orderCode);
}
