package in.fixna.platform.common.web;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Brute-force guard for the public auth surface ({@code /api/v1/auth/**}):
 * per IP + route sliding window; on exhaustion responds 429 with the standard
 * envelope and a Retry-After hint. X-Forwarded-For is honored for caller IP
 * behind the platform's trusted reverse proxy (documented in the operations
 * guide); rate limiting is a mitigation layer, not the credential control.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger RATE_LOG = LoggerFactory.getLogger("fixna.ratelimit");

    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final RateLimiter limiter;
    private final ObjectMapper mapper;

    /**
     * Spring constructor. Explicitly {@code @Autowired} because the
     * package-visible test seam below gives the class two constructors —
     * without the annotation Spring cannot pick one and fails with
     * "No default constructor found" during full-context boots.
     */
    @Autowired
    public RateLimitFilter(
            @Value("${fixna.security.rate-limit.per-ip-per-minute:20}") int perIpPerMinute) {
        this.limiter = new RateLimiter(perIpPerMinute, Duration.ofMinutes(1));
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    // Package-visible seam for unit tests (custom quota, shared clock reality).
    RateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(AUTH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request) + "|" + request.getRequestURI();
        if (!limiter.allow(key)) {
            long retryAfter = limiter.retryAfterSeconds(key);
            String requestId = MDC.get(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
            RATE_LOG.warn("Rate limit exceeded route={} retryAfter={}s", request.getRequestURI(), retryAfter);
            ApiError error = new ApiError(
                    OffsetDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests — please retry later",
                    request.getRequestURI(),
                    requestId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), error);
            return;
        }
        filterChain.doFilter(request, response);
    }

    // Package-visible seam for tests (no Spring context needed).
    static String keyFor(String clientIp, String route) {
        return clientIp + "|" + route;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",", 2)[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    private static String maskIp(String ip) {
        if (ip == null) {
            return "unknown";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        int lastColon = ip.lastIndexOf(':');
        if (lastColon > 0) {
            return ip.substring(0, lastColon) + ":xxxx";
        }
        return "xxx";
    }
}
