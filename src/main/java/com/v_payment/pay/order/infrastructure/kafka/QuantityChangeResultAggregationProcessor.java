package com.v_payment.pay.order.infrastructure.kafka;

import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryMessage;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

public class QuantityChangeResultAggregationProcessor implements Processor<String, QuantityChangeResultMessage, String, QuantityChangeSummaryMessage> {
    private final String storeName;
    private ProcessorContext<String, QuantityChangeSummaryMessage> context;
    private KeyValueStore<String, QuantityChangeResultAggregationState> store;

    public QuantityChangeResultAggregationProcessor(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(ProcessorContext<String, QuantityChangeSummaryMessage> context) {
        this.context = context;
        this.store = context.getStateStore(storeName);
    }

    @Override
    public void process(Record<String, QuantityChangeResultMessage> record) {
        QuantityChangeResultMessage message = record.value();
        if (message == null) return;

        String orderCode = message.orderCode();

        QuantityChangeResultAggregationState state = store.get(orderCode);
        if (state == null) state = new QuantityChangeResultAggregationState();

        state.add(message);

        if (state.isCompleted()) {
            QuantityChangeSummaryMessage summary =
                    new QuantityChangeSummaryMessage(
                            state.orderCode(),
                            state.summaryStatus(),
                            state.successProductIds(),
                            state.failedProductIds(),
                            state.successQuantities(),
                            state.failedQuantities()
                    );

            context.forward(record.withKey(orderCode).withValue(summary));
            store.delete(orderCode);
            return;
        }

        store.put(orderCode, state);
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
