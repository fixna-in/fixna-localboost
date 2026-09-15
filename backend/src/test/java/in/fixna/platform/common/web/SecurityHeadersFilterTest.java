package in.fixna.platform.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Secure headers on every response; HSTS only when the deployment opts in. */
class SecurityHeadersFilterTest {

    @Test
    void setsExpectedHeadersWithoutHstsByDefault() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/campaigns");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("0");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void setsHstsWhenEnabledAndSkipsApiCacheHeaderOffApiPaths() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
        assertThat(response.getHeader("Cache-Control")).isNull();
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }
}