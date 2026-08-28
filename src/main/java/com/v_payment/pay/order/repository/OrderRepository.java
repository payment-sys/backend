package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Query("""
            update Order o
            set o.isFailed = true
            where o.orderCode = :orderCode
              and o.isFailed = false
            """)
    int markFailed(String orderCode);

    @Modifying
    @Query("""
            update Order o
            set o.isFailed = true
            where o.orderCode in :orderCodes
              and o.isFailed = false
            """)
    int markFailedByOrderCodes(Collection<String> orderCodes);
}
