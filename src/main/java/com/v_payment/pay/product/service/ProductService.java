package com.v_payment.pay.product.service;

import com.v_payment.pay.product.controller.dto.req.ProductCreateReq;
import com.v_payment.pay.product.controller.dto.res.ProductCreateRes;
import com.v_payment.pay.product.domain.QuantityChangePlan;
import com.v_payment.pay.product.domain.event.QuantityChangeResultOutboxesEvent;
import com.v_payment.pay.product.domain.entity.ChangeStatus;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeMessage;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeType;
import com.v_payment.pay.product.repository.ProductRepository;
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
public class ProductService {
    private final Clock clock;
    private final ProductRepository productRepository;
    private final QuantityChangeResultOutboxRepository quantityChangeResultOutboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional("productTransactionManager")
    public ProductCreateRes create(ProductCreateReq req) {
        Product product = Product.create(req.name(), req.price(), req.stockQuantity());
        productRepository.save(product);
        return ProductCreateRes.from(product);
    }

    @Transactional("productTransactionManager")
    public void changeQuantityBatch(List<QuantityChangeMessage> quantityChangeMessages) {
        List<QuantityChangeMessage> decreaseMessages = quantityChangeMessages.stream()
                .filter(message -> message.type() == QuantityChangeType.DECREASE)
                .toList();
        List<QuantityChangeMessage> compensateMessages = quantityChangeMessages.stream()
                .filter(message -> message.type() == QuantityChangeType.COMPENSATE)
                .toList();

        compensate(compensateMessages);
        if (decreaseMessages.isEmpty()) {
            return;
        }

        QuantityChangePlan plan = QuantityChangePlan.create(decreaseMessages);
        List<Product> products = productRepository.findAllByIdInForUpdate(plan.getProductIds());
        plan.updateSuccessAndFail(products);
        List<QuantityChangeResultOutbox> insertedOutboxes = createOutboxesByPlan(plan);
        productRepository.changeQuantityBatch(getSuccessChangeQuantities(insertedOutboxes));
        if (!insertedOutboxes.isEmpty()) {
            applicationEventPublisher.publishEvent(new QuantityChangeResultOutboxesEvent(insertedOutboxes));
        }
    }

    private void compensate(List<QuantityChangeMessage> compensateMessages) {
        if (compensateMessages.isEmpty()) {
            return;
        }

        Map<Long, Integer> compensateQuantities = compensateMessages.stream()
                .collect(Collectors.toMap(
                        QuantityChangeMessage::productId,
                        QuantityChangeMessage::changeCount,
                        Integer::sum
                ));
        productRepository.changeQuantityBatch(compensateQuantities);
    }

    private List<QuantityChangeResultOutbox> createOutboxesByPlan(QuantityChangePlan plan) {
        List<QuantityChangeResultOutbox> outboxes = new ArrayList<>();
        for (QuantityChangeMessage message : plan.getSuccess()) {
            outboxes.add(QuantityChangeResultOutbox.success(message, LocalDateTime.now(clock)));
        }
        for (QuantityChangeMessage message : plan.getFail()) {
            outboxes.add(QuantityChangeResultOutbox.fail(message, "OUT_OF_STOCK", LocalDateTime.now(clock)));
        }
        return quantityChangeResultOutboxRepository.createOutboxBatch(outboxes);
    }

    private Map<Long, Integer> getSuccessChangeQuantities(List<QuantityChangeResultOutbox> outboxes) {
        return outboxes.stream()
                .filter(outbox -> outbox.getChangeStatus() == ChangeStatus.SUCCESS)
                .collect(Collectors.toMap(
                        QuantityChangeResultOutbox::getProductId,
                        QuantityChangeResultOutbox::getChangeCount,
                        Integer::sum
                ));
    }
}
