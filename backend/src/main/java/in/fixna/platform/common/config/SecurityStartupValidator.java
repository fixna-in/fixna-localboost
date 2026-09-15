package in.fixna.platform.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails server startup when the {@code prod} profile is active but the JWT
 * signing secret is missing, blank, the bundled dev placeholder, or shorter
 * than 32 bytes. Dev/demo keeps the placeholder but gets a startup warning.
 * Automated contract: SecurityStartupValidatorTest.
 */
@Component
public class SecurityStartupValidator implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityStartupValidator.class);

    static final String DEV_PLACEHOLDER =
            "dev-only-secret-change-me-in-production-32bytes-minimum!!";
    static final int MIN_SECRET_BYTES = 32;

    private final Environment environment;

    public SecurityStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = environment.getActiveProfiles();
        String secret = environment.getProperty("fixna.security.jwt.secret", "");
        verifySecret(java.util.Arrays.asList(profiles), secret);
    }

    static void verifySecret(java.util.List<String> profiles, String secret) {
        boolean prod = profiles != null && profiles.contains("prod");
        if (isWeak(secret)) {
            if (prod) {
                throw new IllegalStateException(
                        "Refusing to start with a missing/default/weak FIXNA_JWT_SECRET on the prod profile");
            }
            LOG.warn("Using a missing/placeholder/weak JWT secret — set FIXNA_JWT_SECRET"
                    + " before any shared deployment");
        }
    }

    private static boolean isWeak(String secret) {
        return secret == null
                || secret.isBlank()
                || secret.equals(DEV_PLACEHOLDER)
                || secret.length() < MIN_SECRET_BYTES;
    }
}
