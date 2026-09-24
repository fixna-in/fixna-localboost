package in.fixna.platform.common.web;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.fixna.platform.common.observability.TelemetryContext;

/**
 * Emits one structured access line per API request to {@code fixna.access}:
 * {@code method=POST path=/api/v1/campaigns status=201 durationMs=12
 * requestId=... tenantId=... userId=...}. Only the request URI is logged —
 * never query strings, headers, bodies, tokens or credentials. Registered
 * inside the JWT filter in the security chain so tenant/user MDC set during
 * authentication is visible, and the final status (total wall time, filters
 * included) is still captured in {@code finally}.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("fixna.access");

    /** Only application traffic is logged; actuator/swagger/static stay quiet. */
    private static final String API_PREFIX = "/api/v1/";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Instant startedAt = Instant.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.getRequestURI() != null && request.getRequestURI().startsWith(API_PREFIX)) {
                ACCESS_LOG.info(
                        "method={} path={} status={} durationMs={} requestId={} tenantId={} userId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        Duration.between(startedAt, Instant.now()).toMillis(),
                        TelemetryContext.requestId(),
                        TelemetryContext.tenantId(),
                        TelemetryContext.userId());
            }
        }
    }
}