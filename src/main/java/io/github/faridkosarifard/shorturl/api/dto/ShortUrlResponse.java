package io.github.faridkosarifard.shorturl.api.dto;

import java.time.Instant;

public record ShortUrlResponse(
        String code,
        String shortUrl,
        String url,
        Instant createdAt,
        Instant expiresAt,
        long clickCount) {}
