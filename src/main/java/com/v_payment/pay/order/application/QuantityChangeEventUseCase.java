package com.v_payment.pay.order.application;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuantityChangeEventUseCase {
    private final Clock clock;
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDoneBatch(List<Long> outboxIds) {
        if (outboxIds == null || outboxIds.isEmpty()) return;
        int doneCount = quantityChangeOutboxRepository.markDoneBatch(outboxIds, LocalDateTime.now(clock));
        if (doneCount != outboxIds.size()) {
            log.warn("outbox DONE batch update 가 실패했습니다. requested={}, updated={}", outboxIds.size(), doneCount);
        }
    }
}
