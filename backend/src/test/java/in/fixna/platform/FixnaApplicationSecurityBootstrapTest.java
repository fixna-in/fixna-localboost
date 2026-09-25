package in.fixna.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/** JWT API must not enable Spring's default generated user/password. */
class FixnaApplicationSecurityBootstrapTest {

    @Test
    void defaultUserDetailsServiceAutoConfigIsExcluded() {
        assertThat(FixnaApplication.class.getAnnotation(
                        org.springframework.boot.autoconfigure.SpringBootApplication.class)
                .exclude())
                .contains(UserDetailsServiceAutoConfiguration.class);
    }
}
