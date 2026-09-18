package com.v_payment.pay.order.service;

import com.v_payment.pay.order.config.OrderExpirationProperties;
import com.v_payment.pay.order.domain.entity.OrderStatus;
import com.v_payment.pay.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Service
@RequiredArgsConstructor
public class OrderExpirationService {
    private final Clock clock;
    private final OrderExpirationProperties orderExpirationProperties;
    private final OrderRepository orderRepository;
    private final OrderManager orderManager;

    @Transactional
    public int expireCreatedOrders() {
        LocalDateTime orderedBefore = LocalDateTime.now(clock)
                .minusSeconds(orderExpirationProperties.expireAfterSeconds());
        List<String> orderCodes = orderRepository.findExpirableCreatedOrderCodes(
                OrderStatus.CREATED,
                orderedBefore,
                PageRequest.of(0, orderExpirationProperties.batchSize())
        );

        if (orderCodes.isEmpty()) return 0;

        boolean updated = orderManager.markExpired(orderCodes);
        if (!updated) {
            log.error("주문 만료 상태 업데이트를 실패했습니다. orderCodes={}", orderCodes);
            throw new IllegalStateException("주문 만료 상태 업데이트를 실패했습니다.");
        }
        return orderCodes.size();
    }
}
