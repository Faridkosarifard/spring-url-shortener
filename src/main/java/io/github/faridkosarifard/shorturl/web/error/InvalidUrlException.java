package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public class InvalidUrlException extends ShortUrlException {

    public InvalidUrlException(String detail) {
        super(HttpStatus.BAD_REQUEST, "Invalid URL", detail);
    }
}
