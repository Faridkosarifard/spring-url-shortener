package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public class ShortUrlExpiredException extends ShortUrlException {

    public ShortUrlExpiredException(String code) {
        super(HttpStatus.GONE, "Short URL expired", "The short URL '%s' has expired".formatted(code));
    }
}
