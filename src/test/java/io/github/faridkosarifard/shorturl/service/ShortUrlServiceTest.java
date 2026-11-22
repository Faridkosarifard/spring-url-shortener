package io.github.faridkosarifard.shorturl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    private ShortUrlProperties properties;
    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        properties = new ShortUrlProperties(
                "http://localhost:8080/",
                7,
                3,
                302,
                false,
                List.of(),
                List.of("api", "swagger-ui"),
                new ShortUrlProperties.RateLimit(true, 60, false));
        UrlValidator validator = new UrlValidator(properties);
        service = new ShortUrlService(
                repository, codeGenerator, validator, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsGeneratedCodeAndSkipsCollisions() {
        when(codeGenerator.generate(7)).thenReturn("taken01", "okCode1");
        when(repository.existsByCode("taken01")).thenReturn(true);
        when(repository.existsByCode("okCode1")).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response =
                service.create(new CreateShortUrlRequest("https://example.com/a", null, null, null));

        assertThat(response.code()).isEqualTo("okCode1");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/okCode1");
        assertThat(response.url()).isEqualTo("https://example.com/a");
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.expiresAt()).isNull();
        assertThat(response.clickCount()).isZero();
    }

    @Test
    void usesCustomAliasAndExpiryInDays() {
        when(repository.existsByCode("docs")).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response =
                service.create(new CreateShortUrlRequest("https://example.com/docs", "docs", 10, null));

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(repository).save(captor.capture());
        verify(codeGenerator, never()).generate(anyInt());
        assertThat(response.code()).isEqualTo("docs");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plus(10, ChronoUnit.DAYS));
    }

    @Test
    void prefersExplicitExpiryTimestamp() {
        Instant expiresAt = NOW.plus(2, ChronoUnit.HOURS);
        when(repository.existsByCode("meet")).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response =
                service.create(new CreateShortUrlRequest("https://example.com", "meet", 30, expiresAt));

        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void rejectsReservedAndTakenAliases() {
        assertThatThrownBy(() ->
                        service.create(new CreateShortUrlRequest("https://example.com", "api", null, null)))
                .isInstanceOf(AliasUnavailableException.class)
                .hasMessageContaining("reserved");

        when(repository.existsByCode("blog")).thenReturn(true);
        assertThatThrownBy(() ->
                        service.create(new CreateShortUrlRequest("https://example.com", "blog", null, null)))
                .isInstanceOf(AliasUnavailableException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void mapsUniqueConstraintToAliasConflict() {
        when(codeGenerator.generate(7)).thenReturn("abc1234");
        when(repository.existsByCode("abc1234")).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() ->
                        service.create(new CreateShortUrlRequest("https://example.com", null, null, null)))
                .isInstanceOf(AliasUnavailableException.class);
    }

    @Test
    void failsWhenCodesCannotBeAllocated() {
        when(codeGenerator.generate(7)).thenReturn("taken01");
        when(repository.existsByCode("taken01")).thenReturn(true);

        assertThatThrownBy(() ->
                        service.create(new CreateShortUrlRequest("https://example.com", null, null, null)))
                .isInstanceOf(ShortCodeAllocationException.class);
    }

    @Test
    void rejectsExpiryInThePast() {
        assertThatThrownBy(() -> service.create(
                        new CreateShortUrlRequest("https://example.com", null, null, NOW.minusSeconds(1))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void resolveIncrementsClicksAndRejectsMissingOrExpired() {
        ShortUrl live = new ShortUrl("live01", "https://example.com", NOW, NOW.plus(1, ChronoUnit.DAYS));
        when(repository.findByCode("live01")).thenReturn(Optional.of(live));

        assertThat(service.resolveForRedirect("live01").getTargetUrl()).isEqualTo("https://example.com");
        verify(repository).incrementClickCount("live01");

        when(repository.findByCode("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolveForRedirect("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);

        ShortUrl expired = new ShortUrl("old01", "https://example.com", NOW.minus(5, ChronoUnit.DAYS), NOW);
        when(repository.findByCode("old01")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resolveForRedirect("old01"))
                .isInstanceOf(ShortUrlExpiredException.class);
        verify(repository, never()).incrementClickCount("old01");
    }

    @Test
    void getAndDeleteRequireAnExistingCode() {
        ShortUrl live = new ShortUrl("live01", "https://example.com", NOW, null);
        when(repository.findByCode("live01")).thenReturn(Optional.of(live));

        assertThat(service.get("live01").code()).isEqualTo("live01");
        service.delete("live01");
        verify(repository).delete(live);

        when(repository.findByCode("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("missing")).isInstanceOf(ShortUrlNotFoundException.class);
        assertThatThrownBy(() -> service.delete("missing")).isInstanceOf(ShortUrlNotFoundException.class);
    }
}
