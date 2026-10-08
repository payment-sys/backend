package com.v_payment.pay.order.service;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import com.v_payment.pay.order.repository.QuantityChangeOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuantityChangesEventService {
    private final Clock clock;
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDone(Long outboxId) {
        int doneCount = quantityChangeOutboxRepository.markDone(
                outboxId,
                QuantityChangeOutboxStatus.READY,
                QuantityChangeOutboxStatus.DONE,
                LocalDateTime.now(clock)
        );

        if(doneCount != 1) log.warn("아웃박스 발행 성공 상태 변경 실패하였습니다. outboxId={}", outboxId);
    }
}
