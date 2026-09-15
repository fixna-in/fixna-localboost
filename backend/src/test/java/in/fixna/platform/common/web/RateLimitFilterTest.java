package in.fixna.platform.common.web;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** RateLimitFilter: scopes to the auth surface, 429s over quota, safe under race. */
class RateLimitFilterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static MockHttpServletRequest request(String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void limitsArePerIpAndRoute() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimiter(1, Duration.ofMinutes(1)));
        CountingChain chain = new CountingChain();

        filter.doFilter(request("/api/v1/auth/login", "10.0.0.1"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("/api/v1/auth/login", "10.0.0.1"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("/api/v1/auth/login", "10.0.0.2"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("/api/v1/auth/refresh", "10.0.0.1"), new MockHttpServletResponse(), chain);

        assertThat(chain.hits).isEqualTo(3);
    }

    @Test
    void overQuotaYieldsStandardEnvelopeWithRetryAfter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimiter(1, Duration.ofMinutes(1)));
        CountingChain chain = new CountingChain();

        filter.doFilter(request("/api/v1/auth/login", "10.0.0.9"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/auth/login", "10.0.0.9"), blocked, chain);

        assertThat(chain.hits).isEqualTo(1);
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getContentType()).contains("application/json");
        JsonNode body = mapper.readTree(blocked.getContentAsByteArray());
        assertThat(body.get("code").asText()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void nonAuthPathsAreNeverLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new RateLimiter(1, Duration.ofMinutes(1)));
        CountingChain chain = new CountingChain();

        for (int i = 0; i < 10; i++) {
            filter.doFilter(
                    request("/api/v1/campaigns", "10.0.0.7"), new MockHttpServletResponse(), chain);
        }

        assertThat(chain.hits).isEqualTo(10);
    }

    @Test
    void concurrentBurstStaysWithinQuota() throws Exception {
        int quota = 20;
        RateLimitFilter filter = new RateLimitFilter(new RateLimiter(quota, Duration.ofMinutes(1)));
        CountingChain chain = new CountingChain();
        int threads = 40;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    filter.doFilter(
                            request("/api/v1/auth/login", "10.0.0.5"),
                            new MockHttpServletResponse(), chain);
                    return null;
                });
            }
            ready.await();
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(chain.hits).isLessThanOrEqualTo(quota);
        assertThat(chain.hits).isGreaterThan(0);
    }

    private static final class CountingChain implements FilterChain {
        int hits;

        @Override
        public synchronized void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            hits++;
        }
    }
}