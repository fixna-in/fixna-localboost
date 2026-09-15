package in.fixna.platform.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings. Defaults are dev/demo only — production must set
 * {@code FIXNA_JWT_SECRET} (min 256-bit) via environment.
 */
@ConfigurationProperties(prefix = "fixna.security.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            secret = "dev-only-secret-change-me-in-production-32bytes-minimum!!";
        }
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(7);
        }
    }
}
