package in.fixna.platform.common.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Workflow 13 — production fail-fast guard for the data plane. On the
 * {@code prod} profile the application refuses to start unless the PostgreSQL
 * datasource is explicitly configured (never the bundled localhost default)
 * with non-placeholder credentials. Redis is optional in the MVP, so a
 * missing override only logs a warning. The JWT secret is guarded separately
 * by {@link SecurityStartupValidator}.
 */
@Component
public class ProdEnvironmentValidator implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ProdEnvironmentValidator.class);

    static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/fixna";
    static final String DEFAULT_DB_PASSWORD = "change-me";

    private final Environment environment;

    public ProdEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || Arrays.stream(profiles).noneMatch("prod"::equals)) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        String username = environment.getProperty("spring.datasource.username", "");
        String password = environment.getProperty("spring.datasource.password", "");

        if (isBlank(url) || url.equals(DEFAULT_DB_URL) || url.contains("localhost")) {
            throw illegal("spring.datasource.url must point at the production PostgreSQL"
                    + " instance (SPRING_DATASOURCE_URL) - refusing to fall back to the local default: " + url);
        }
        if (isBlank(username)) {
            throw illegal("spring.datasource.username is required in prod (POSTGRES_USER)");
        }
        if (isBlank(password) || password.equals(DEFAULT_DB_PASSWORD)) {
            throw illegal("spring.datasource.password is missing or still the default in prod (POSTGRES_PASSWORD)");
        }
        if (isDefaultRedis()) {
            LOG.warn("REDIS_URL is not set — Redis-backed features are disabled in this deployment");
        }
    }

    private boolean isDefaultRedis() {
        // Base config (WF13) uses spring.data.redis.host/port; keep the
        // legacy url key as a fallback so either style suppresses the warning.
        String host = environment.getProperty("spring.data.redis.host", "");
        String url = environment.getProperty("spring.data.redis.url", "");
        return (host.isBlank() || host.contains("localhost"))
                && (url.isBlank() || url.contains("localhost"));
    }

    private static IllegalStateException illegal(String message) {
        return new IllegalStateException("Refusing to start: " + message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}