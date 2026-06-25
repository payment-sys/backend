package com.v_payment.pay.payment.service.outbox.queue;

import com.v_payment.pay.payment.entity.outbox.PaymentOutbox;
import com.v_payment.pay.payment.service.outbox.PaymentOutboxTask;

import java.util.List;

public interface OutboxQueue {
    void enqueue(PaymentOutbox paymentOutbox);

    void notifyEnqueued();

    void listenEnqueued();

    List<PaymentOutboxTask> poll(int count);

    record OutboxEnqueueEvent() {}
}
