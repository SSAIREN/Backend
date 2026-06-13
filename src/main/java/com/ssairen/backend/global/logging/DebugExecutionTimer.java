package com.ssairen.backend.global.logging;

import java.util.function.Supplier;
import org.slf4j.Logger;

public final class DebugExecutionTimer {

    private DebugExecutionTimer() {
    }

    public static <T> T measure(Logger log, String category, String operation, String details, Supplier<T> supplier) {
        long startedAt = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            log(log, category, operation, details, startedAt);
        }
    }

    public static void measure(Logger log, String category, String operation, String details, Runnable runnable) {
        long startedAt = System.nanoTime();
        try {
            runnable.run();
        } finally {
            log(log, category, operation, details, startedAt);
        }
    }

    private static void log(Logger log, String category, String operation, String details, long startedAt) {
        if (!log.isDebugEnabled()) {
            return;
        }

        double elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0;
        if (details == null || details.isBlank()) {
            log.debug("[perf] category={} operation={} elapsedMs={}", category, operation, String.format("%.3f", elapsedMs));
            return;
        }

        log.debug(
                "[perf] category={} operation={} elapsedMs={} details={}",
                category,
                operation,
                String.format("%.3f", elapsedMs),
                details
        );
    }
}
