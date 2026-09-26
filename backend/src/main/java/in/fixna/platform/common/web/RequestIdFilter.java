package in.fixna.platform.common.web;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import in.fixna.platform.common.logging.LoggingConstants;
import in.fixna.platform.common.logging.LoggingContext;

/**
 * Propagates or mints an {@code X-Request-Id} correlation id per request.
 * Available to handlers via request attribute, response header and MDC so
 * logs, error envelopes and audit events can be correlated end to end.
 *
 * <p>Outermost in the security chain (see {@code SecurityConfig}): MDC is
 * populated before the chain and cleared in {@code finally}, so tenant/user
 * MDC set by inner filters remains visible to the access log.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = LoggingConstants.REQUEST_ID_HEADER;
    public static final String REQUEST_ID_ATTRIBUTE = LoggingConstants.REQUEST_ID;

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_\\-:]{1,64}");

    private final Optional<Tracer> tracer;

    /** {@code Optional} so Spring injects empty when no {@link Tracer} bean is configured. */
    public RequestIdFilter(Optional<Tracer> tracer) {
        this.tracer = tracer != null ? tracer : Optional.empty();
    }

    /** Package-visible for unit tests without a Spring context. */
    static RequestIdFilter forTests(Tracer tracer) {
        return new RequestIdFilter(Optional.ofNullable(tracer));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(REQUEST_ID_HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        LoggingContext.putRequestId(requestId);
        populateTraceContext();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoggingContext.clearRequest();
        }
    }

    private void populateTraceContext() {
        tracer.flatMap(active -> Optional.ofNullable(active.currentSpan()))
                .ifPresent(span -> LoggingContext.putTrace(
                        span.context().traceId(), span.context().spanId()));
    }
    
    /** Accepts only safe incoming ids; returns null when a fresh id must be minted. */
    static String sanitize(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return null;
        }
        String trimmed = incoming.trim();
        return SAFE_ID.matcher(trimmed).matches() ? trimmed : null;
    }
}
