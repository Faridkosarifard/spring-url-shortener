package io.github.faridkosarifard.shorturl.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    @Test
    void allowsUpToLimitThenRejectsUntilWindowResets() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2, Duration.ofMinutes(1), clock);

        InMemoryRateLimiter.Result first = limiter.tryAcquire("10.0.0.1");
        InMemoryRateLimiter.Result second = limiter.tryAcquire("10.0.0.1");
        InMemoryRateLimiter.Result third = limiter.tryAcquire("10.0.0.1");

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
        assertThat(second.remaining()).isZero();
        assertThat(third.allowed()).isFalse();
        assertThat(third.retryAfter()).isPositive();

        clock.set(Instant.parse("2026-01-01T00:01:00Z"));
        assertThat(limiter.tryAcquire("10.0.0.1").allowed()).isTrue();
    }

    @Test
    void tracksKeysIndependently() {
        InMemoryRateLimiter limiter =
                new InMemoryRateLimiter(1, Duration.ofMinutes(1), Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
    }

    @Test
    void rejectsNonPositiveLimit() {
        assertThatThrownBy(() -> new InMemoryRateLimiter(0, Duration.ofMinutes(1), Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
