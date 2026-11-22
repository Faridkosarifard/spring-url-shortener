package io.github.faridkosarifard.shorturl.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShortUrlTest {

    @Test
    void treatsExpiryAtOrBeforeNowAsExpired() {
        Instant now = Instant.parse("2026-06-01T00:00:00Z");
        ShortUrl open = new ShortUrl("a", "https://example.com", now, null);
        ShortUrl untilNow = new ShortUrl("b", "https://example.com", now, now);
        ShortUrl later = new ShortUrl("c", "https://example.com", now, now.plusSeconds(1));

        assertThat(open.isExpired(now)).isFalse();
        assertThat(untilNow.isExpired(now)).isTrue();
        assertThat(later.isExpired(now)).isFalse();
        assertThat(open.getClickCount()).isZero();
    }

    @Test
    void equalityUsesPersistentIdentity() {
        Instant now = Instant.parse("2026-06-01T00:00:00Z");
        ShortUrl first = new ShortUrl("a", "https://example.com", now, null);
        ShortUrl second = new ShortUrl("b", "https://example.com", now, null);

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isEqualTo(first);
        assertThat(first).isNotEqualTo("a");
    }
}
