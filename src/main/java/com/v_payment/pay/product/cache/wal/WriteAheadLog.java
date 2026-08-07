package com.v_payment.pay.product.cache.wal;

import com.v_payment.pay.product.service.ProductManager;
import com.v_payment.pay.product.service.ProductManager.ReserveProduct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class WriteAheadLog {
    private static final String RESERVE = "RESERVE";
    private static final String RESTORE = "RESTORE";
    private static final String FIELD_SEPARATOR_REGEX = "\\|";
    private static final String FIELD_SEPARATOR = "|";
    private static final String ITEM_SEPARATOR = ",";
    private static final String ITEM_FIELD_SEPARATOR = ":";

    private final Path path;
    private final AtomicLong version = new AtomicLong();

    public WriteAheadLog(
            @Value("${product.cache.wal.path:./data/product-reservation.wal}") String path
    ) {
        this.path = Path.of(path);
        initialize();
    }

    public Entry reserve(List<ReserveProduct> reserveProducts) {
        return write(RESERVE, toReserveItems(reserveProducts));
    }

    public Entry restore(List<ProductManager.ProductRestoreReq> requests) {
        return write(RESTORE, toRestoreItems(requests));
    }

    private Entry write(String type, List<Item> items) {
        Entry entry = new Entry(
                nextVersion(),
                type,
                items
        );
        append(entry);
        return entry;
    }

    public List<Entry> entries() {
        try {
            if (Files.notExists(path)) return List.of();

            return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank())
                    .map(this::readEntry)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read product reservation WAL.", e);
        }
    }

    private long nextVersion() {
        return version.incrementAndGet();
    }

    private List<Item> toReserveItems(List<ReserveProduct> reserveProducts) {
        return reserveProducts.stream()
                .map(product -> new Item(product.productId(), product.quantity()))
                .toList();
    }

    private List<Item> toRestoreItems(List<ProductManager.ProductRestoreReq> requests) {
        return requests.stream()
                .map(request -> new Item(request.productId(), request.quantity()))
                .toList();
    }

    private void initialize() {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            if (Files.notExists(path)) Files.createFile(path);

            version.set(readLastVersion());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize product reservation WAL.", e);
        }
    }

    private long readLastVersion() throws IOException {
        long lastVersion = 0L;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            lastVersion = Math.max(lastVersion, readEntry(line).version());
        }
        return lastVersion;
    }

    private synchronized void append(Entry entry) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            channel.write(ByteBuffer.wrap(toBytes(entry)));
            channel.force(true);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append product reservation WAL.", e);
        }
    }

    private byte[] toBytes(Entry entry) {
        return (serialize(entry) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
    }

    private Entry readEntry(String line) {
        String[] fields = line.split(FIELD_SEPARATOR_REGEX, -1);
        if (fields.length != 3) {
            throw new IllegalStateException("Invalid product reservation WAL entry: " + line);
        }

        return new Entry(
                Long.parseLong(fields[0]),
                fields[1],
                readItems(fields[2])
        );
    }

    private String serialize(Entry entry) {
        return entry.version()
                + FIELD_SEPARATOR
                + entry.type()
                + FIELD_SEPARATOR
                + serializeItems(entry.items());
    }

    private String serializeItems(List<Item> items) {
        return items.stream()
                .map(item -> item.productId() + ITEM_FIELD_SEPARATOR + item.quantity())
                .collect(Collectors.joining(ITEM_SEPARATOR));
    }

    private List<Item> readItems(String value) {
        if (value.isBlank()) return List.of();

        List<Item> items = new ArrayList<>();
        for (String item : value.split(ITEM_SEPARATOR)) {
            items.add(readItem(item));
        }
        return List.copyOf(items);
    }

    private Item readItem(String value) {
        String[] fields = value.split(ITEM_FIELD_SEPARATOR, -1);
        if (fields.length != 2) {
            throw new IllegalStateException("Invalid product reservation WAL item: " + value);
        }
        return new Item(Long.parseLong(fields[0]), Integer.parseInt(fields[1]));
    }

    public record Entry(
            Long version,
            String type,
            List<Item> items
    ) {
    }

    public record Item(
            Long productId,
            Integer quantity
    ) {
    }
}
