package in.fixna.platform.common.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Secure response headers for every response. CSP is intentionally absent: a
 * global {@code default-src 'none'} would break the self-served Swagger UI,
 * while the API only ever returns JSON (X-Content-Type-Options: nosniff +
 * X-Frame-Options: DENY carry the protection this API needs). Strict
 * transport security is enabled by property on TLS-terminated hosts only.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final boolean hstsEnabled;

    public SecurityHeadersFilter(@Value("${fixna.security.hsts-enabled:false}") boolean hstsEnabled) {
        this.hstsEnabled = hstsEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-XSS-Protection", "0");
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/v1")) {
            // Responses may carry JWTs; they must never be cached.
            response.setHeader("Cache-Control", "no-store");
        }
        if (hstsEnabled) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }
}
