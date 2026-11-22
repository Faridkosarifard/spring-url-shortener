package io.github.faridkosarifard.shorturl.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.faridkosarifard.shorturl.config.ShortUrlProperties;
import io.github.faridkosarifard.shorturl.web.error.InvalidUrlException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UrlValidatorTest {

    private final UrlValidator blockingPrivate = validator(true, List.of("blocked.test"));
    private final UrlValidator allowingPrivate = validator(false, List.of("blocked.test"));

    @Test
    void acceptsPublicHttpsUrl() {
        assertThat(blockingPrivate.validate(" https://example.com/path?q=1 "))
                .isEqualTo("https://example.com/path?q=1");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ftp://example.com/file",
                "javascript:alert(1)",
                "data:text/plain,hi",
                "example.com/no-scheme",
                "/relative",
                "https://user:pass@example.com/secret"
            })
    void rejectsDisallowedUrls(String url) {
        assertThatThrownBy(() -> blockingPrivate.validate(url)).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsBlankAndOversizedUrls() {
        assertThatThrownBy(() -> blockingPrivate.validate("  ")).isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> blockingPrivate.validate("https://example.com/" + "a".repeat(2048)))
                .isInstanceOf(InvalidUrlException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://localhost/admin",
                "http://localhost.localdomain/",
                "http://app.localhost/",
                "http://127.0.0.1/",
                "http://10.0.0.5/",
                "http://192.168.1.10/",
                "http://172.16.4.1/",
                "http://169.254.1.1/",
                "http://100.64.0.1/",
                "http://[::1]/",
                "http://[fc00::1]/",
                "http://[fe80::1]/"
            })
    void rejectsPrivateAndLocalHostsWhenEnabled(String url) {
        assertThatThrownBy(() -> blockingPrivate.validate(url)).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void allowsPrivateHostsWhenDisabled() {
        assertThat(allowingPrivate.validate("http://127.0.0.1/health")).isEqualTo("http://127.0.0.1/health");
    }

    @Test
    void rejectsBlockedHostsAndSubdomains() {
        assertThatThrownBy(() -> blockingPrivate.validate("https://blocked.test/x"))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> blockingPrivate.validate("https://evil.blocked.test/x"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsInvalidUri() {
        assertThatThrownBy(() -> blockingPrivate.validate("https://exam ple.com"))
                .isInstanceOf(InvalidUrlException.class);
    }

    private static UrlValidator validator(boolean blockPrivate, List<String> blockedHosts) {
        ShortUrlProperties properties = new ShortUrlProperties(
                "http://localhost:8080",
                7,
                8,
                302,
                blockPrivate,
                blockedHosts,
                List.of("api"),
                new ShortUrlProperties.RateLimit(true, 60, false));
        return new UrlValidator(properties);
    }
}
