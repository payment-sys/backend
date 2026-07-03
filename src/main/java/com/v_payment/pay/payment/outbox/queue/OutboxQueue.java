package com.v_payment.pay.payment.outbox.queue;

import com.v_payment.pay.payment.outbox.PaymentOutboxTask;
import com.v_payment.pay.payment.outbox.entity.PaymentOutbox;

import java.util.List;

public interface OutboxQueue {
    void enqueue(PaymentOutbox paymentOutbox);

    void notifyEnqueued();

    void listenEnqueued();

    List<PaymentOutboxTask> poll(int count);

    record OutboxEnqueueEvent() {}
}
