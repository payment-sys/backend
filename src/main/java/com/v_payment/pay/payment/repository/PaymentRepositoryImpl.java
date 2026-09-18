package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentBatchRepository {
    private static final String READY_PAYMENT_INSERT_SQL = """
            insert into payment (
                provider,
                payment_method,
                order_code,
                idempotency_key,
                payment_key,
                requested_amount,
                approved_amount,
                payment_status,
                requested_at,
                approved_at,
                receipt_url,
                recovery_attempt_count
            ) values (?, ?, ?, ?, null, ?, null, ?, ?, null, null, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveReadyPayments(Collection<Payment> payments) {
        if (payments.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                READY_PAYMENT_INSERT_SQL,
                payments,
                payments.size(),
                this::bindReadyPayment
        );
    }

    private void bindReadyPayment(PreparedStatement ps, Payment payment) throws SQLException {
        ps.setString(1, payment.getProvider().name());
        ps.setString(2, payment.getPaymentMethod().name());
        ps.setString(3, payment.getOrderCode());
        ps.setString(4, payment.getIdempotencyKey());
        ps.setLong(5, payment.getRequestedAmount());
        ps.setString(6, payment.getPaymentStatus().name());
        ps.setObject(7, payment.getRequestedAt());
        ps.setInt(8, payment.getRecoveryAttemptCount());
    }
}
