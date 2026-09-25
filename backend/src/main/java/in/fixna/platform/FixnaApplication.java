package in.fixna.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * JWT API — no Spring default user/password (UserDetailsServiceAutoConfiguration disabled).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FixnaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FixnaApplication.class, args);
    }
}
