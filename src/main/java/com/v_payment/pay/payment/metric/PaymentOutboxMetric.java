package com.v_payment.pay.payment.metric;

import com.v_payment.pay.payment.entity.Payment;

import com.v_payment.pay.global.meter.CounterMeter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOutboxMetric {
    private final CounterMeter paymentQueueEnqueueCounter;
    private final CounterMeter paymentQueueCompleteCounter;

    public void incrementEnqueued() {
        paymentQueueEnqueueCounter.increment();
    }

    public void incrementCompleted(long count) {
        if (count > 0) {
            paymentQueueCompleteCounter.increment(count);
        }
    }
}
