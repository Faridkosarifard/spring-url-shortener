package io.github.faridkosarifard.shorturl.support;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window counter. Suitable for a single application instance.
 */
public class InMemoryRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final Duration windowSize;
    private final Clock clock;

    public InMemoryRateLimiter(int limit, Duration windowSize, Clock clock) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
        this.windowSize = windowSize;
        this.clock = clock;
    }

    public Result tryAcquire(String key) {
        long now = clock.millis();
        long windowStart = now - (now % windowSize.toMillis());
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.startMillis != windowStart) {
                return new Window(windowStart);
            }
            return existing;
        });
        int count = window.count.incrementAndGet();
        evictOccasionally(now);
        int remaining = Math.max(0, limit - count);
        Duration retryAfter = Duration.ofMillis(windowStart + windowSize.toMillis() - now);
        boolean allowed = count <= limit;
        return new Result(allowed, remaining, allowed ? Duration.ZERO : retryAfter, limit);
    }

    private void evictOccasionally(long now) {
        if ((now & 0x3F) != 0) {
            return;
        }
        long staleBefore = now - (2 * windowSize.toMillis());
        windows.entrySet().removeIf(entry -> entry.getValue().startMillis < staleBefore);
    }

    public record Result(boolean allowed, int remaining, Duration retryAfter, int limit) {}

    private static final class Window {
        private final long startMillis;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
