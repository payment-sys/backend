package com.v_payment.pay.product.mq;

import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MqLog {
    private static final Path LOG_PATH = Path.of("logs", "product-quantity-mq.log");

    private final Clock clock;

    public synchronized void append(ProductQuantityEventPayload payload) {
        appendLine(toLogLine(payload));
    }

    public synchronized void append(String status, List<ProductQuantityEventPayload> payloads) {
        appendLine(toLogLine(status, payloads));
    }

    public synchronized void append(String message) {
        appendLine(LocalDateTime.now(clock) + " " + message);
    }

    private String toLogLine(ProductQuantityEventPayload payload) {
        return LocalDateTime.now(clock)
                + " orderCode=" + payload.getOrderCode()
                + " paymentMethod=" + payload.getPaymentMethod()
                + " requestedQuantities=" + payload.getRequestedQuantities();
    }

    private String toLogLine(String status, List<ProductQuantityEventPayload> payloads) {
        return LocalDateTime.now(clock)
                + " status=" + status
                + " batchSize=" + payloads.size()
                + " orderCodes=" + payloads.stream()
                .map(ProductQuantityEventPayload::getOrderCode)
                .toList();
    }

    private void appendLine(String line) {
        try {
            Path parent = LOG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    LOG_PATH,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IllegalStateException("MQ log append failed.", e);
        }
    }
}
