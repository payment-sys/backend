package com.v_payment.pay.payment.outbox;

import com.v_payment.pay.global.meter.CounterMeter;
import com.v_payment.pay.global.meter.TimerMeter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PaymentOutboxMetric {
    private final CounterMeter paymentQueueEnqueueCounter;
    private final CounterMeter paymentQueueClaimCounter;
    private final CounterMeter paymentQueueRetryCounter;
    private final CounterMeter paymentQueueCompleteCounter;
    private final CounterMeter paymentQueueDiscardCounter;
    private final TimerMeter paymentQueueRunTimer;
    private final TimerMeter virtualThreadRunTimer;

    public void incrementEnqueued() {
        paymentQueueEnqueueCounter.increment();
    }

    public void incrementClaimed(long count) {
        if (count > 0) {
            paymentQueueClaimCounter.increment(count);
        }
    }

    public void incrementRetryScheduled(long count) {
        if (count > 0) {
            paymentQueueRetryCounter.increment(count);
        }
    }

    public void incrementCompleted(long count) {
        if (count > 0) {
            paymentQueueCompleteCounter.increment(count);
        }
    }

    public void incrementDiscarded(long count) {
        if (count > 0) {
            paymentQueueDiscardCounter.increment(count);
        }
    }

    public void recordSchedulerCycle(long elapsedNanos) {
        paymentQueueRunTimer.record(Duration.ofNanos(elapsedNanos));
    }

    public void recordTaskElapsed(long elapsedNanos) {
        virtualThreadRunTimer.record(Duration.ofNanos(elapsedNanos));
    }
}
