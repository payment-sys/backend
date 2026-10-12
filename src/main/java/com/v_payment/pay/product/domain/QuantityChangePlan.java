package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;

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

    public static QuantityChangePlan calculate(
            Map<Long, ProductSnapShot> productSnapShots,
            QuantityChangeMessages messages
    ) {
        QuantityChangePlan plan = new QuantityChangePlan(messages.getAllMessages());
        for (Map.Entry<Long, List<QuantityChangeMessage>> entry : plan.messageMap.entrySet()) {
            Long productId = entry.getKey();
            List<QuantityChangeMessage> productMessages = entry.getValue();
            ProductSnapShot productSnapShot = productSnapShots.get(productId);

            if (productSnapShot == null) {
                plan.fail.addAll(productMessages);
                continue;
            }

            for (QuantityChangeMessage message : productMessages) {
                if (!productSnapShot.canChange(message.changeCount())) {
                    plan.fail.add(message);
                    continue;
                }
                plan.success.add(message);
                productSnapShot.change(message.changeCount());
            }
        }

        return plan;
    }

    private List<QuantityChangeMessage> validateOriginMessages(List<QuantityChangeMessage> messages) {
        if(messages == null) throw new IllegalStateException("messages는 없을 수 없습니다.");
        return messages;
    }

    public List<QuantityChangeMessage> getSuccess() {
        return new ArrayList<>(success);
    }

    public List<QuantityChangeMessage> getFail() {
        return new ArrayList<>(fail);
    }
}
