package io.github.faridkosarifard.shorturl.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "shorturl")
public record ShortUrlProperties(
        @NotBlank String baseUrl,
        @Min(4) @Max(16) int codeLength,
        @Min(1) @Max(64) int maxCodeAttempts,
        @Min(301) @Max(302) int redirectStatus,
        boolean blockPrivateHosts,
        List<String> blockedHosts,
        List<String> reservedCodes,
        @NotNull @Valid RateLimit rateLimit) {

    public ShortUrlProperties {
        baseUrl = trimTrailingSlash(baseUrl);
        blockedHosts = normalizeHosts(blockedHosts);
        reservedCodes = normalizeCodes(reservedCodes);
        if (redirectStatus != 301 && redirectStatus != 302) {
            throw new IllegalArgumentException("redirect-status must be 301 or 302");
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(true, 60, false);
        }
    }

    public String shortLink(String code) {
        return baseUrl + "/" + code;
    }

    public boolean isReserved(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return reservedCodes.contains(code.toLowerCase(Locale.ROOT));
    }

    public boolean isBlockedHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = stripTrailingDot(host.toLowerCase(Locale.ROOT));
        for (String blocked : blockedHosts) {
            if (normalized.equals(blocked) || normalized.endsWith("." + blocked)) {
                return true;
            }
        }
        return false;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static List<String> normalizeHosts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> stripTrailingDot(value.strip().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static List<String> normalizeCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.strip().toLowerCase(Locale.ROOT))
                .toList();
    }

    private static String stripTrailingDot(String host) {
        if (host.endsWith(".")) {
            return host.substring(0, host.length() - 1);
        }
        return host;
    }

    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @Min(1) @DefaultValue("60") int requestsPerMinute,
            @DefaultValue("false") boolean trustForwardedHeaders) {}
}
