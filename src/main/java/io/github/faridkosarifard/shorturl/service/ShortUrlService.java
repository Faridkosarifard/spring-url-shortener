package io.github.faridkosarifard.shorturl.service;

import io.github.faridkosarifard.shorturl.api.dto.CreateShortUrlRequest;
import io.github.faridkosarifard.shorturl.api.dto.ShortUrlResponse;
import io.github.faridkosarifard.shorturl.config.ShortUrlProperties;
import io.github.faridkosarifard.shorturl.domain.ShortUrl;
import io.github.faridkosarifard.shorturl.repository.ShortUrlRepository;
import io.github.faridkosarifard.shorturl.support.ShortCodeGenerator;
import io.github.faridkosarifard.shorturl.support.UrlValidator;
import io.github.faridkosarifard.shorturl.web.error.AliasUnavailableException;
import io.github.faridkosarifard.shorturl.web.error.InvalidRequestException;
import io.github.faridkosarifard.shorturl.web.error.ShortCodeAllocationException;
import io.github.faridkosarifard.shorturl.web.error.ShortUrlExpiredException;
import io.github.faridkosarifard.shorturl.web.error.ShortUrlNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final UrlValidator urlValidator;
    private final ShortUrlProperties properties;
    private final Clock clock;

    public ShortUrlService(
            ShortUrlRepository repository,
            ShortCodeGenerator codeGenerator,
            UrlValidator urlValidator,
            ShortUrlProperties properties,
            Clock clock) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.urlValidator = urlValidator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        String targetUrl = urlValidator.validate(request.url());
        Instant now = clock.instant();
        Instant expiresAt = resolveExpiry(request, now);
        String code = resolveCode(request);
        ShortUrl entity = new ShortUrl(code, targetUrl, now, expiresAt);
        try {
            return toResponse(repository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new AliasUnavailableException("Alias is already in use");
        }
    }

    @Transactional
    public ShortUrl resolveForRedirect(String code) {
        ShortUrl entity = repository
                .findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));
        if (entity.isExpired(clock.instant())) {
            throw new ShortUrlExpiredException(code);
        }
        repository.incrementClickCount(code);
        return entity;
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse get(String code) {
        ShortUrl entity = repository
                .findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));
        return toResponse(entity);
    }

    @Transactional
    public void delete(String code) {
        ShortUrl entity = repository
                .findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));
        repository.delete(entity);
    }

    private String resolveCode(CreateShortUrlRequest request) {
        String alias = request.alias();
        if (alias != null && !alias.isBlank()) {
            String code = alias.strip();
            if (properties.isReserved(code)) {
                throw new AliasUnavailableException("Alias is reserved");
            }
            if (repository.existsByCode(code)) {
                throw new AliasUnavailableException("Alias is already in use");
            }
            return code;
        }
        return allocateCode();
    }

    private String allocateCode() {
        int length = properties.codeLength();
        for (int attempt = 0; attempt < properties.maxCodeAttempts(); attempt++) {
            String code = codeGenerator.generate(length);
            if (!properties.isReserved(code) && !repository.existsByCode(code)) {
                return code;
            }
        }
        throw new ShortCodeAllocationException();
    }

    private Instant resolveExpiry(CreateShortUrlRequest request, Instant now) {
        if (request.expiresAt() != null) {
            if (!request.expiresAt().isAfter(now)) {
                throw new InvalidRequestException(
                        "expiresAt must be in the future");
            }
            return request.expiresAt();
        }
        if (request.expiresInDays() != null) {
            return now.plus(request.expiresInDays(), ChronoUnit.DAYS);
        }
        return null;
    }

    private ShortUrlResponse toResponse(ShortUrl entity) {
        return new ShortUrlResponse(
                entity.getCode(),
                properties.shortLink(entity.getCode()),
                entity.getTargetUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getClickCount());
    }
}
