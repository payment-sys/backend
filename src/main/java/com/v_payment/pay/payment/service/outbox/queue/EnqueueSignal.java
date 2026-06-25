package com.v_payment.pay.payment.service.outbox.queue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EnqueueSignal {
    private final Runnable callback;

    public void notifyEnqueue() {
    }

    public void callBack() {
    }
}
