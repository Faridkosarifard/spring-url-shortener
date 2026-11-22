package io.github.faridkosarifard.shorturl.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecureRandomShortCodeGeneratorTest {

    private final SecureRandomShortCodeGenerator generator = new SecureRandomShortCodeGenerator();

    @Test
    void generatesBase62CodeOfRequestedLength() {
        String code = generator.generate(7);

        assertThat(code).hasSize(7).matches("[0-9A-Za-z]{7}");
    }

    @Test
    void generatesDifferentCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(generator.generate(8));
        }

        assertThat(codes.size()).isGreaterThan(190);
    }

    @Test
    void rejectsNonPositiveLength() {
        assertThatThrownBy(() -> generator.generate(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
