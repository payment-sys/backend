package com.v_payment.pay.global;

import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class HeapDumpTool {
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean;
    private final Path dumpDirectory;
    private final boolean live;
    private final AtomicBoolean dumped = new AtomicBoolean(false);
    private final AtomicLong sequence = new AtomicLong(0);

    public HeapDumpTool(
            @Value("${diagnostics.heap-dump.directory:/tmp/heap-dumps}") String dumpDirectory,
            @Value("${diagnostics.heap-dump.live:false}") boolean live
    ) {
        this.hotSpotDiagnosticMXBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        this.dumpDirectory = Path.of(dumpDirectory);
        this.live = live;
    }

    public Optional<Path> dumpOnce(String reason) {
        if (!dumped.compareAndSet(false, true)) {
            return Optional.empty();
        }

        try {
            return Optional.of(dump(reason));
        } catch (RuntimeException e) {
            dumped.set(false);
            throw e;
        }
    }

    public Path dump(String reason) {
        try {
            Files.createDirectories(dumpDirectory);
            Path dumpPath = nextDumpPath(reason);
            hotSpotDiagnosticMXBean.dumpHeap(dumpPath.toAbsolutePath().toString(), live);
            log.warn("Heap dump completed. path={} live={}", dumpPath.toAbsolutePath(), live);
            return dumpPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create heap dump", e);
        }
    }

    private Path nextDumpPath(String reason) {
        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        long pid = ProcessHandle.current().pid();
        long dumpSequence = sequence.incrementAndGet();
        return dumpDirectory.resolve("heap-" + sanitize(reason) + "-" + timestamp + "-pid" + pid + "-" + dumpSequence + ".hprof");
    }

    private String sanitize(String reason) {
        if (reason == null || reason.isBlank()) {
            return "manual";
        }

        String sanitized = reason.strip()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (sanitized.isBlank()) {
            return "manual";
        }
        return sanitized;
    }
}
