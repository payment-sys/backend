package com.v_payment.pay.product.mq;

import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class ProductQuantityMessageQueue {
    private final BlockingQueue<ProductQuantityEventPayload> queue = new LinkedBlockingQueue<>();

    public List<ProductQuantityEventPayload> consumePayloads(int size) throws InterruptedException {
        ProductQuantityEventPayload first = queue.take();

        List<ProductQuantityEventPayload> payloads = new ArrayList<>(size);
        payloads.add(first);

        queue.drainTo(payloads, size - 1);
        return payloads;
    }

    public boolean add(ProductQuantityEventPayload payload) {
        return queue.offer(payload);
    }
}