package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public class ShortUrlNotFoundException extends ShortUrlException {

    public ShortUrlNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, "Short URL not found", "No short URL exists for code '%s'".formatted(code));
    }
}
