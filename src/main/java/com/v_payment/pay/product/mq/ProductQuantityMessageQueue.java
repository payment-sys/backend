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

        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);

        while (payloads.size() < size) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) break;

            ProductQuantityEventPayload next = queue.poll(remaining, TimeUnit.NANOSECONDS);
            if (next == null) break;

            payloads.add(next);
            queue.drainTo(payloads, size - payloads.size());
        }

        return payloads;
    }

    public boolean add(ProductQuantityEventPayload payload) {
        return queue.offer(payload);
    }
}