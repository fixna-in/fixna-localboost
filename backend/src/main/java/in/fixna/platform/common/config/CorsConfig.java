package in.fixna.platform.common.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration source for the security chain. Exact origins only (never
 * wildcards) because credentials/authorization are accepted; applies to
 * {@code /api/**}. An empty allowlist produces a source with no origins, so
 * cross-origin requests are denied by default (fail closed).
 */
@Configuration
public class CorsConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        List<String> origins = properties.allowedOrigins().stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty() && !origin.equals("*"))
                .toList();
        CorsConfiguration config = new CorsConfiguration();
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        } else if (!properties.allowedOrigins().isEmpty()) {
            // An allowlist that collapses to nothing (blank entries or the "*"
            // wildcard with credentials) refuses every cross-origin call —
            // log it so the misconfiguration is visible in operations.
            LOG.warn("CORS allowlist collapsed to no origins after filtering — "
                    + "cross-origin calls will be refused until FIXNA_CORS_ALLOWED_ORIGINS is fixed");
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
