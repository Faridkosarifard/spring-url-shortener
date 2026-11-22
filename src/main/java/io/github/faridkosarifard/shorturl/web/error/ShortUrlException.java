package io.github.faridkosarifard.shorturl.web.error;

import org.springframework.http.HttpStatus;

public abstract class ShortUrlException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected ShortUrlException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}
