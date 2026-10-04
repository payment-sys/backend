package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaMetrics {
    private final MeterRegistry meterRegistry;

    public <T> CompletableFuture<T> recordProducerSend(String topic, CompletableFuture<T> sendResult) {
        return sendResult.whenComplete((unused, ex) -> {
            String result = ex == null ? "success" : "failure";
            Counter.builder("pay.kafka.producer.send")
                    .description("Kafka producer send result count")
                    .tag("topic", topic)
                    .tag("result", result)
                    .register(meterRegistry)
                    .increment();
        });
    }

    public void recordConsumerProcess(String topic, String consumer, Runnable task) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            task.run();
        } catch (Exception e) {
            result = "failure";
            throw e;
        } finally {
            Counter.builder("pay.kafka.consumer.process")
                    .description("Kafka consumer process result count")
                    .tag("topic", topic)
                    .tag("consumer", consumer)
                    .tag("result", result)
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder("pay.kafka.consumer.process.duration")
                    .description("Kafka consumer process duration")
                    .tag("topic", topic)
                    .tag("consumer", consumer)
                    .tag("result", result)
                    .register(meterRegistry));
        }
    }
}
