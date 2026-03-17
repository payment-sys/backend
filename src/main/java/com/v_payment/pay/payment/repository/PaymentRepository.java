package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}
