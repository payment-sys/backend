package com.v_payment.pay.product.service;

import com.v_payment.pay.product.exception.ProductQuantityEventConsumeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQuantityEventFacade {
    private final ProductQuantityEventService productQuantityEventService;

    public void consumeReadyEvent(int batchSize) {
        try {
            productQuantityEventService.consumeReadyEvent(batchSize);
        } catch (ProductQuantityEventConsumeException e) {
            log.error("db consumer failed. eventIds={}", e.getEventIds(), e);
            productQuantityEventService.markRetry(e.getEvents(), e.getEventIds());
        } catch (Exception e) {
            log.error("상품 수량 이벤트 처리 중 알 수 없는 오류가 발생했습니다.", e);
        }
    }

    public void consumeRetryEvent(int batchSize) {
        try {
            productQuantityEventService.consumeRetryEvent(batchSize);
        } catch (ProductQuantityEventConsumeException e) {
            log.error("db retry consumer failed. eventIds={}", e.getEventIds(), e);
            productQuantityEventService.markRetry(e.getEvents(), e.getEventIds());
        } catch (Exception e) {
            log.error("상품 수량 재시도 이벤트 처리 중 알 수 없는 오류가 발생했습니다.", e);
        }
    }
}
