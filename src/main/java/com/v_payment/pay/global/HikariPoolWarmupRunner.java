package com.v_payment.pay.global;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class HikariPoolWarmupRunner implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
            return;
        }

        int targetSize = Math.min(
                hikariDataSource.getMinimumIdle(),
                hikariDataSource.getMaximumPoolSize()
        );
        if (targetSize <= 0) {
            return;
        }

        List<Connection> connections = new ArrayList<>(targetSize);
        try {
            for (int i = 0; i < targetSize; i++) {
                connections.add(hikariDataSource.getConnection());
            }
            log.info("HikariCP warm-up completed. warmedConnections={}", connections.size());
        } finally {
            SQLException closeException = null;
            for (Connection connection : connections) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (closeException == null) {
                        closeException = e;
                    } else {
                        closeException.addSuppressed(e);
                    }
                }
            }
            if (closeException != null) {
                throw closeException;
            }
        }
    }
}
