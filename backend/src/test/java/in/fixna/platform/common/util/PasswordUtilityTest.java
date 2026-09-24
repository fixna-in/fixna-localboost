package in.fixna.platform.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordUtilityTest {
    @Test
    void hashesAreSaltedAndCompatibleWithLoginEncoder() {
        String password = "Demo-password-123!";
        String hash = PasswordUtility.hash(password);
        assertThat(hash).isNotEqualTo(PasswordUtility.hash(password));
        assertThat(new BCryptPasswordEncoder().matches(password, hash)).isTrue();
        assertThat(PasswordUtility.verify(password, hash)).isTrue();
        assertThat(PasswordUtility.verify("Wrong-password-123!", hash)).isFalse();
    }

    @Test
    void rejectsInvalidPasswordsWithoutEchoingThem() {
        for (String password : new String[] {"short", " ".repeat(8), "x".repeat(73), "é".repeat(37)}) {
            assertThatThrownBy(() -> PasswordUtility.hash(password))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageNotContaining(password);
        }
        assertThatThrownBy(() -> PasswordUtility.hash(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsBcryptByteBoundary() {
        String password = "é".repeat(36);
        assertThat(PasswordUtility.verify(password, PasswordUtility.hash(password))).isTrue();
    }

    @Test
    void rejectsMalformedOrExcessiveCostHash() {
        for (String hash : new String[] {"not-a-hash", "$2a$31$" + "a".repeat(53), ""}) {
            assertThatThrownBy(() -> PasswordUtility.verify("Demo-password-123!", hash))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
