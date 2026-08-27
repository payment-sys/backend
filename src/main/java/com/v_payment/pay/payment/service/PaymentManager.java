package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class PaymentManager {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    public void createPendingPayment(String orderCode, Long amount, PaymentMethod paymentMethod) {
        paymentRepository.save(Payment.createPendingPayment(orderCode, amount, paymentMethod, clock));
    }

    public void createPendingPayments(Collection<PendingPaymentCreateRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }

        LocalDateTime requestedAt = LocalDateTime.now(clock);
        jdbcTemplate.batchUpdate(
                """
                insert into payment (
                    provider,
                    payment_method,
                    order_code,
                    payment_key,
                    requested_amount,
                    approved_amount,
                    payment_status,
                    requested_at,
                    approved_at,
                    receipt_url,
                    recovery_attempt_count
                ) values (?, ?, ?, null, ?, null, ?, ?, null, null, 0)
                """,
                requests,
                requests.size(),
                (ps, request) -> {
                    ps.setString(1, Provider.TOSS.name());
                    ps.setString(2, request.paymentMethod().name());
                    ps.setString(3, request.orderCode());
                    ps.setLong(4, request.amount());
                    ps.setString(5, PaymentStatus.READY.name());
                    ps.setObject(6, requestedAt);
                }
        );
    }

    public record PendingPaymentCreateRequest(
            String orderCode,
            Long amount,
            PaymentMethod paymentMethod
    ) {
    }
}
