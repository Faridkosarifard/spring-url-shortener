package io.github.faridkosarifard.shorturl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "target_url", nullable = false, columnDefinition = "TEXT")
    private String targetUrl;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ShortUrl() {}

    public ShortUrl(String code, String targetUrl, Instant createdAt, Instant expiresAt) {
        this.code = code;
        this.targetUrl = targetUrl;
        this.clickCount = 0L;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ShortUrl that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
