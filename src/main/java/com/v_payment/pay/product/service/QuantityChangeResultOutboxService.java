package com.v_payment.pay.product.service;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutboxStatus;
import com.v_payment.pay.product.repository.QuantityChangeResultOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuantityChangeResultOutboxService {
    private final Clock clock;
    private final QuantityChangeResultOutboxRepository quantityChangeResultOutboxRepository;

    @Transactional
    public void markDoneBatch(List<QuantityChangeResultOutbox> outboxes) {
        if (outboxes == null || outboxes.isEmpty()) {
            return;
        }

        quantityChangeResultOutboxRepository.markDoneBatch(
                outboxes,
                QuantityChangeResultOutboxStatus.READY,
                QuantityChangeResultOutboxStatus.DONE,
                LocalDateTime.now(clock)
        );
    }
}
