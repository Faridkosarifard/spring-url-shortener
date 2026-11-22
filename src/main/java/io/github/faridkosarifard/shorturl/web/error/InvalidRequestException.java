package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ShortUrlException {

    public InvalidRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }
}
