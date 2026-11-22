package io.github.faridkosarifard.shorturl.support;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureRandomShortCodeGenerator implements ShortCodeGenerator {

    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be positive");
        }
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(buffer);
    }
}
