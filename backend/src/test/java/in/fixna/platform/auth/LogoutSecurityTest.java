package in.fixna.platform.auth;

import java.util.Optional;
import java.util.UUID;

import in.fixna.platform.common.config.SecurityConfig;
import in.fixna.platform.common.web.*;
import in.fixna.platform.tenant.MembershipRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MVC/security chain without a database; only authentication services are mocked. */
class LogoutSecurityTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private AuthService auth;
    private JwtService jwt;
    private MembershipLookup memberships;
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
        auth = context.getBean(AuthService.class);
        jwt = context.getBean(JwtService.class);
        memberships = context.getBean(MembershipLookup.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void logoutUsesVerifiedIdentity() throws Exception {
        when(jwt.verify("test-access", "access"))
                .thenReturn(new JwtService.VerifiedToken(userId, tenantId, "TENANT_OWNER"));
        when(memberships.roleFor(tenantId, userId)).thenReturn(Optional.of(MembershipRole.TENANT_OWNER));
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer test-access"))
                .andExpect(status().isNoContent());
        verify(auth).logout(userId, tenantId);
    }

    @Test
    void missingTokenReturnsUnauthorizedWithoutCallingService() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        verifyNoInteractions(auth, jwt, memberships);
    }

    @Test
    void invalidTokenReturnsUnauthorizedWithoutCallingService() throws Exception {
        when(jwt.verify("invalid", "access")).thenThrow(new InvalidTokenException("Invalid token"));
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(auth, memberships);
    }

    @Test
    void revokedMembershipCannotLogout() throws Exception {
        when(jwt.verify("test-access", "access"))
                .thenReturn(new JwtService.VerifiedToken(userId, tenantId, "TENANT_OWNER"));
        when(memberships.roleFor(tenantId, userId)).thenReturn(Optional.empty());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer test-access"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }

    @Test
    void publicAuthEndpointsStillReachValidationWithoutAccessToken() throws Exception {
        for (String endpoint : new String[] {"register", "login", "refresh"}) {
            mvc.perform(post("/api/v1/auth/" + endpoint).contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(jwt, memberships);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, AuthController.class, JwtAuthenticationFilter.class,
            RequestIdFilter.class, RequestLoggingFilter.class, SecurityHeadersFilter.class,
            RateLimitFilter.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
    static class TestConfig {
        @Bean AuthService authService() { return mock(AuthService.class); }
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean MembershipLookup membershipLookup() { return mock(MembershipLookup.class); }
    }
}
