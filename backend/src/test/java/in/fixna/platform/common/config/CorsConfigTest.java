package in.fixna.platform.common.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/** CORS: exact-origin allowlist on /api/**, unlisted paths unconfigured. */
class CorsConfigTest {

    private final CorsConfig config = new CorsConfig();

    @Test
    void allowsOnlyConfiguredOriginsWithCredentials() {
        CorsProperties properties =
                new CorsProperties(List.of("https://app.fixna.in", "https://admin.fixna.in"));
        CorsConfigurationSource source = config.corsConfigurationSource(properties);

        CorsConfiguration resolved = source.getCorsConfiguration(apiRequest("https://app.fixna.in"));
        assertThat(resolved).isNotNull();
        assertThat(resolved.getAllowedOrigins())
                .containsExactly("https://app.fixna.in", "https://admin.fixna.in");
        assertThat(resolved.getAllowCredentials()).isTrue();
        assertThat(resolved.getAllowedHeaders()).contains("Authorization", "X-Request-Id");
        assertThat(resolved.getExposedHeaders()).contains("X-Request-Id");

        CorsConfiguration foreign = source.getCorsConfiguration(apiRequest("https://evil.example"));
        assertThat(foreign.checkOrigin("https://evil.example")).isNull();
    }

    @Test
    void wildcardsAreNeverAccepted() {
        CorsConfigurationSource source =
                config.corsConfigurationSource(new CorsProperties(List.of("*")));

        assertThat(source.getCorsConfiguration(apiRequest("https://app.fixna.in"))
                        .checkOrigin("https://app.fixna.in"))
                .isNull();
    }

    @Test
    void nonApiPathsHaveNoCorsConfiguration() {
        CorsConfigurationSource source =
                config.corsConfigurationSource(new CorsProperties(List.of("https://app.fixna.in")));

        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/actuator/health")))
                .isNull();
    }

    private static MockHttpServletRequest apiRequest(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/campaigns");
        request.addHeader("Origin", origin);
        return request;
    }
}