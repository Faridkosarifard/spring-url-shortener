package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public class ShortCodeAllocationException extends ShortUrlException {

    public ShortCodeAllocationException() {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Short code unavailable",
                "Unable to allocate a unique short code. Try again.");
    }
}
