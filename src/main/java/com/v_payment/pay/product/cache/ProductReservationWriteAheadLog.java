package com.v_payment.pay.product.cache;

import com.v_payment.pay.product.cache.dto.WriteAheadLogEntry;
import com.v_payment.pay.product.cache.dto.WriteAheadLogItem;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
public class ProductReservationWriteAheadLog {
    private final Object fileLock = new Object();
    private final AtomicLong version = new AtomicLong();

    private final Path walPath;
    private final ObjectMapper objectMapper;

    public ProductReservationWriteAheadLog(
            @Value("${product.cache.wal.path}") String walPath,
            ObjectMapper objectMapper
    ) {
        this.walPath = Path.of(walPath);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initialize() {
        ensureParentDirectory();
        version.set(findLastVersion());
    }

    public WriteAheadLogEntry append(Map<Long, Integer> deltasByProductId) {
        List<WriteAheadLogItem> items = deltasByProductId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new WriteAheadLogItem(entry.getKey(), entry.getValue()))
                .toList();

        return append(items);
    }

    public WriteAheadLogEntry append(List<WriteAheadLogItem> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("WAL items must not be empty.");

        synchronized (fileLock) {
            WriteAheadLogEntry entry = new WriteAheadLogEntry(
                    version.incrementAndGet(),
                    List.copyOf(items)
            );
            appendEntry(entry);
            return entry;
        }
    }

    public List<WriteAheadLogEntry> replay() {
        synchronized (fileLock) {
            return readEntries();
        }
    }

    public void clear() {
        synchronized (fileLock) {
            try (FileChannel channel = FileChannel.open(
                    walPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                channel.force(true);
                version.set(0);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to clear product reservation WAL.", e);
            }
        }
    }

    private void appendEntry(WriteAheadLogEntry entry) {
        try (FileChannel channel = FileChannel.open(
                walPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        )) {
            byte[] bytes = (objectMapper.writeValueAsString(entry) + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to append product reservation WAL.", e);
        }
    }

    private List<WriteAheadLogEntry> readEntries() {
        if (Files.notExists(walPath)) return List.of();

        try {
            List<String> lines = Files.readAllLines(walPath, StandardCharsets.UTF_8);
            return parseEntries(lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read product reservation WAL.", e);
        }
    }

    private List<WriteAheadLogEntry> parseEntries(List<String> lines) {
        return lines.stream()
                .filter(line -> !line.isBlank())
                .map(this::parseEntry)
                .toList();
    }

    private WriteAheadLogEntry parseEntry(String line) {
        try {
            return objectMapper.readValue(line, WriteAheadLogEntry.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to parse product reservation WAL entry.", e);
        }
    }

    private long findLastVersion() {
        return replay().stream()
                .map(WriteAheadLogEntry::version)
                .max(Comparator.naturalOrder())
                .orElse(0L);
    }

    private void ensureParentDirectory() {
        Path parent = walPath.toAbsolutePath().getParent();
        if (parent == null) return;

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create product reservation WAL directory.", e);
        }
    }

}
