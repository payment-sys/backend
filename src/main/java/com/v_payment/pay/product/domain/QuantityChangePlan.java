package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeMessage;

import java.util.*;
import java.util.stream.Collectors;

public class QuantityChangePlan {
    private final List<QuantityChangeMessage> success = new ArrayList<>();
    private final List<QuantityChangeMessage> fail = new ArrayList<>();
    private final Map<Long, List<QuantityChangeMessage>> messageMap = new HashMap<>();

    private QuantityChangePlan(List<QuantityChangeMessage> messages) {
        List<QuantityChangeMessage> quantityChangeMessages = validateOriginMessages(messages);
        for (QuantityChangeMessage msg : quantityChangeMessages) {
            messageMap.computeIfAbsent(msg.productId(), k -> new ArrayList<>()).add(msg);
        }
    }

    public Map<Long, Integer> getSuccessChangeQuantities() {
        return success.stream().collect(Collectors.toMap(
                        QuantityChangeMessage::productId,
                        QuantityChangeMessage::changeCount, Integer::sum
                ));
    }

    public Collection<Long> getProductIds() {
        return messageMap.keySet();
    }

    public void updateSuccessAndFail(List<Product> products) {
        Map<Long, Integer> remainQuantities = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getStockQuantity));

        for (Map.Entry<Long, List<QuantityChangeMessage>> entry : messageMap.entrySet()) {
            Long productId = entry.getKey();
            List<QuantityChangeMessage> messages = entry.getValue();
            Integer remainQuantity = remainQuantities.get(productId);

            if (remainQuantity == null) {
                fail.addAll(messages);
                continue;
            }

            for (QuantityChangeMessage message : messages) {
                int nextQuantity = remainQuantity + message.changeCount();
                if (nextQuantity < 0) {
                    fail.add(message);
                    continue;
                }
                success.add(message);
                remainQuantity = nextQuantity;
            }
        }
    }

    public static QuantityChangePlan create(List<QuantityChangeMessage> messages) {
        return new QuantityChangePlan(messages);
    }

    private List<QuantityChangeMessage> validateOriginMessages(List<QuantityChangeMessage> messages) {
        if(messages == null || messages.size() == 0) throw new IllegalStateException("messages는 없을 수 없습니다.");
        return messages;
    }

    public List<QuantityChangeMessage> getSuccess() {
        return new ArrayList<>(success);
    }

    public List<QuantityChangeMessage> getFail() {
        return new ArrayList<>(fail);
    }
}
