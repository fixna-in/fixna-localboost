package in.fixna.platform.common.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Startup guard: prod must never run on the dev placeholder or a short secret. */
class SecurityStartupValidatorTest {

    private static final String STRONG =
            "this-is-a-rotated-256-bit-secret-0123456789abcdef-keep-safe";

    @Test
    void prodWithPlaceholderRefusesToStart() {
        assertThatThrownBy(() -> SecurityStartupValidator.verifySecret(
                        List.of("prod"), SecurityStartupValidator.DEV_PLACEHOLDER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIXNA_JWT_SECRET");
    }

    @Test
    void prodWithBlankOrShortSecretRefusesToStart() {
        assertThatThrownBy(() -> SecurityStartupValidator.verifySecret(List.of("prod"), ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecurityStartupValidator.verifySecret(List.of("prod"), "short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prodWithStrongSecretStarts() {
        assertThatCode(() -> SecurityStartupValidator.verifySecret(List.of("prod"), STRONG))
                .doesNotThrowAnyException();
    }

    @Test
    void devPlaceholderOnlyWarns() {
        assertThatCode(() -> SecurityStartupValidator.verifySecret(
                        List.of(), SecurityStartupValidator.DEV_PLACEHOLDER))
                .doesNotThrowAnyException();
    }
}