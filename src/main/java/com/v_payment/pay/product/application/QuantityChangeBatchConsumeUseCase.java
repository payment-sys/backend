package com.v_payment.pay.product.application;

import com.v_payment.pay.product.domain.ProductSnapShot;
import com.v_payment.pay.product.domain.QuantityChangeMessages;
import com.v_payment.pay.product.domain.QuantityChangePlan;
import com.v_payment.pay.product.domain.entity.ChangeStatus;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.QuantityChangeLedger;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.domain.event.QuantityChangeResultOutboxesEvent;
import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;
import com.v_payment.pay.product.repository.ProductRepository;
import com.v_payment.pay.product.repository.QuantityChangeLedgerRepository;
import com.v_payment.pay.product.repository.QuantityChangeResultOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuantityChangeBatchConsumeUseCase {
    private final Clock clock;
    private final ProductRepository productRepository;
    private final QuantityChangeLedgerRepository quantityChangeLedgerRepository;
    private final QuantityChangeResultOutboxRepository quantityChangeResultOutboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional("productTransactionManager")
    public void changeQuantityBatch(QuantityChangeMessages messages) {
        //모든 상품찾기 with lock
        List<Product> products = productRepository.findAllByIdInForUpdate(messages.getAllProductIds());
        //스냅샷 생성
        Map<Long, ProductSnapShot> productSnapShots = makeProductSnapShots(products);
        //ledger 조회로 이미 성공 처리된 메시지 제거(멱등)
        messages.deleteNotIdempotencyMessages(findExistLedger(messages));
        //생성 시 성공 실패 나뉨
        QuantityChangePlan quantityChangePlan = QuantityChangePlan.calculate(productSnapShots, messages);
        //성공한 메시지만 원장 생성 batch insert
        createQuantityChangeLedger(quantityChangePlan.getSuccess());
        //보상이 아닐 시 아웃박스 생성(성공, 실패 모두 가능)
        List<QuantityChangeResultOutbox> outboxes = createOutboxesBySuccessPlan(quantityChangePlan);
        //성공 변경만 재고 차감
        productRepository.changeQuantityBatch(quantityChangePlan.getSuccessChangeQuantities());
        if(!outboxes.isEmpty()) {
            applicationEventPublisher.publishEvent(new QuantityChangeResultOutboxesEvent(outboxes));
        }
    }

    private Map<Long, ProductSnapShot> makeProductSnapShots(List<Product> products) {
        return products.stream().collect(Collectors.toMap(Product::getId, ProductSnapShot::from));
    }

    private List<QuantityChangeLedger> findExistLedger(QuantityChangeMessages messages) {
        List<QuantityChangeMessage> quantityChangeMessages = messages.getAllMessages();
        if (quantityChangeMessages.isEmpty()) return List.of();
        return quantityChangeLedgerRepository.findExistingLedgers(quantityChangeMessages);
    }

    private void createQuantityChangeLedger(List<QuantityChangeMessage> successMessages) {
        if (successMessages.isEmpty()) return;
        List<QuantityChangeLedger> ledgers = successMessages.stream()
                .map(message -> QuantityChangeLedger.success(message, LocalDateTime.now(clock)))
                .toList();
        quantityChangeLedgerRepository.createLedgerBatch(ledgers);
    }

    private List<QuantityChangeResultOutbox> createOutboxesBySuccessPlan(QuantityChangePlan plan) {
        List<QuantityChangeResultOutbox> outboxes = new ArrayList<>();
        for (QuantityChangeMessage message : plan.getSuccess()) {
            outboxes.add(QuantityChangeResultOutbox.success(message, LocalDateTime.now(clock)));
        }
        for (QuantityChangeMessage message : plan.getFail()) {
            outboxes.add(QuantityChangeResultOutbox.fail(message, LocalDateTime.now(clock)));
        }
        return quantityChangeResultOutboxRepository.createOutboxBatch(outboxes);
    }
}
