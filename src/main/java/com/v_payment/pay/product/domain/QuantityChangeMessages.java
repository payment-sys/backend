package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;
import com.v_payment.pay.product.domain.entity.QuantityChangeLedger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class QuantityChangeMessages {
    private final Map<Long, List<QuantityChangeMessage>> messages;

    private QuantityChangeMessages(List<QuantityChangeMessage> messages) {
        this.messages = messages.stream().collect(Collectors.groupingBy(QuantityChangeMessage::productId));
    }

    public List<Long> getAllProductIds() {
        return new ArrayList<>(messages.keySet());
    }

    public List<QuantityChangeMessage> getAllMessages() {
        return messages.values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    public void deleteNotIdempotencyMessages(Collection<QuantityChangeLedger> ledgers) {
        if (ledgers == null || ledgers.isEmpty()) return;
        for (QuantityChangeLedger ledger : ledgers) {
            List<QuantityChangeMessage> productMessages = messages.get(ledger.getProductId());
            if (productMessages == null) continue;
            productMessages.removeIf(message -> isSame(ledger, message));
            if (productMessages.isEmpty()) messages.remove(ledger.getProductId());
        }
    }

    private boolean isSame(QuantityChangeLedger ledger, QuantityChangeMessage message) {
        return Objects.equals(message.orderCode(), ledger.getOrderCode())
                && message.type() == ledger.getChangeType();
    }

    public static QuantityChangeMessages from(List<QuantityChangeMessage> messages) {
        return new QuantityChangeMessages(messages);
    }
}
