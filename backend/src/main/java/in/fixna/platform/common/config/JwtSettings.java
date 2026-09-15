package in.fixna.platform.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed security settings bound from {@code fixna.security.*} (Workflow 13).
 * Secrets stay in environment variables; this record only carries the bindings.
 */
@ConfigurationProperties(prefix = "fixna.security.jwt")
public record JwtSettings(String secret, Duration accessTtl, Duration refreshTtl) {

    public JwtSettings {
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(7);
        }
    }
}
