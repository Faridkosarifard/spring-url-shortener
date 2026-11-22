package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public class AliasUnavailableException extends ShortUrlException {

    public AliasUnavailableException(String detail) {
        super(HttpStatus.CONFLICT, "Alias unavailable", detail);
    }
}
