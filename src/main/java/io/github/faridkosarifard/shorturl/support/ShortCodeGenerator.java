package io.github.faridkosarifard.shorturl.support;

public interface ShortCodeGenerator {

    /**
     * Returns a random base62 code of the requested length.
     */
    String generate(int length);
}
