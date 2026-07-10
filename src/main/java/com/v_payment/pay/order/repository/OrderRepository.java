package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
