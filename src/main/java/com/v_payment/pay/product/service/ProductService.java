package com.v_payment.pay.product.service;

import com.v_payment.pay.product.controller.dto.req.ProductCreateReq;
import com.v_payment.pay.product.controller.dto.res.ProductCreateRes;
import com.v_payment.pay.product.domain.QuantityChangePlan;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeMessage;
import com.v_payment.pay.product.repository.ProductRepository;
import com.v_payment.pay.product.repository.QuantityChangeResultOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalEventPublisher;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final Clock clock;
    private final ProductRepository productRepository;
    private final QuantityChangeResultOutboxRepository quantityChangeResultOutboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ProductCreateRes create(ProductCreateReq req) {
        Product product = Product.create(req.name(), req.price(), req.stockQuantity());
        productRepository.save(product);
        return ProductCreateRes.from(product);
    }

    @Transactional
    public void changeQuantityBatch(List<QuantityChangeMessage> quantityChangeMessages) {
        QuantityChangePlan plan = QuantityChangePlan.create(quantityChangeMessages);
        List<Product> products = productRepository.findAllByIdInForUpdate(plan.getProductIds());
        plan.updateSuccessAndFail(products);
        productRepository.changeQuantityBatch(plan.getSuccessChangeQuantities());
        List<QuantityChangeResultOutbox> outboxes = createOutboxesByPlan(plan);
        quantityChangeResultOutboxRepository.createOutboxBatch(outboxes);
        applicationEventPublisher.publishEvent(outboxes);
    }

    private List<QuantityChangeResultOutbox> createOutboxesByPlan(QuantityChangePlan plan) {
        List<QuantityChangeResultOutbox> outboxes = new ArrayList<>();
        for (QuantityChangeMessage message : plan.getSuccess()) {
            outboxes.add(QuantityChangeResultOutbox.success(message, LocalDateTime.now(clock)));
        }
        for (QuantityChangeMessage message : plan.getFail()) {
            outboxes.add(QuantityChangeResultOutbox.fail(message, "OUT_OF_STOCK", LocalDateTime.now(clock)));
        }
        return outboxes;
    }
}
