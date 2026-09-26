package in.fixna.platform.common.config;

import in.fixna.platform.auth.JwtAuthenticationFilter;
import in.fixna.platform.auth.JwtProperties;
import in.fixna.platform.common.web.ApiAccessDeniedHandler;
import in.fixna.platform.common.web.ApiAuthenticationEntryPoint;
import in.fixna.platform.common.web.RateLimitFilter;
import in.fixna.platform.common.web.RequestIdFilter;
import in.fixna.platform.common.web.RequestLoggingFilter;
import in.fixna.platform.common.web.SecurityHeadersFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Workflow 03 security plus Workflow 10 hardening: stateless JWT, CSRF
 * disabled (token-based API), fail-closed on /api/v1 (authenticated), CORS
 * from the configured allowlist, rate limiting on the public auth surface,
 * and secure response headers. Public: auth endpoints, API liveness probe,
 * actuator health/info, OpenAPI docs.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestIdFilter requestIdFilter,
            RequestLoggingFilter requestLoggingFilter,
            SecurityHeadersFilter securityHeadersFilter,
            RateLimitFilter rateLimitFilter,
            JwtAuthenticationFilter jwtFilter,
            ApiAuthenticationEntryPoint entryPoint,
            ApiAccessDeniedHandler deniedHandler)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .addFilterBefore(requestIdFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(securityHeadersFilter, RequestIdFilter.class)
                // Order matters: JWT first (sets TenantContext + tenant/user MDC),
                // then rate limiting (runs authenticated), then the access log so
                // the single fixna.access line sees requestId/tenant/user MDC.
                // JWT must be registered before it can be used as a reference.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/health/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }
}
