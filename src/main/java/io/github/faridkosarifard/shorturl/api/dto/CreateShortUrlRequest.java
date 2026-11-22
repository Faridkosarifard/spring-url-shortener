package io.github.faridkosarifard.shorturl.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateShortUrlRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String alias,
        @Min(1) @Max(3650) Integer expiresInDays,
        @Future Instant expiresAt) {}
