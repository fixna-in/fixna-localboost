package in.fixna.platform.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS allowlist. An empty list refuses all cross-origin calls (fail closed).
 * Dev/demo default is the local Next.js origin; production must set
 * {@code FIXNA_CORS_ALLOWED_ORIGINS} via environment.
 */
@ConfigurationProperties(prefix = "fixna.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of("http://localhost:3000", "http://127.0.0.1:3000")
                : List.copyOf(allowedOrigins);
    }
}
