package io.github.faridkosarifard.shorturl.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShortUrlPropertiesTest {

    @Test
    void normalizesBaseUrlHostsAndReservedCodes() {
        ShortUrlProperties properties = new ShortUrlProperties(
                "http://localhost:8080/",
                7,
                8,
                302,
                true,
                List.of(" Evil.Example ", "", "blocked.test."),
                List.of(" API ", "docs"),
                new ShortUrlProperties.RateLimit(true, 30, false));

        assertThat(properties.baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(properties.shortLink("ab12")).isEqualTo("http://localhost:8080/ab12");
        assertThat(properties.isReserved("api")).isTrue();
        assertThat(properties.isReserved("API")).isTrue();
        assertThat(properties.isReserved("other")).isFalse();
        assertThat(properties.isBlockedHost("evil.example")).isTrue();
        assertThat(properties.isBlockedHost("sub.blocked.test")).isTrue();
        assertThat(properties.isBlockedHost("blocked.test.")).isTrue();
        assertThat(properties.isBlockedHost("example.com")).isFalse();
    }

    @Test
    void defaultsEmptyListsAndRateLimit() {
        ShortUrlProperties properties =
                new ShortUrlProperties("http://localhost:8080", 7, 8, 301, false, null, null, null);

        assertThat(properties.blockedHosts()).isEmpty();
        assertThat(properties.reservedCodes()).isEmpty();
        assertThat(properties.rateLimit().enabled()).isTrue();
        assertThat(properties.isReserved(null)).isFalse();
        assertThat(properties.isBlockedHost(" ")).isFalse();
    }

    @Test
    void rejectsUnsupportedRedirectStatus() {
        assertThatThrownBy(() -> new ShortUrlProperties(
                        "http://localhost:8080",
                        7,
                        8,
                        303,
                        false,
                        List.of(),
                        List.of(),
                        new ShortUrlProperties.RateLimit(true, 60, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redirect-status");
    }
}
