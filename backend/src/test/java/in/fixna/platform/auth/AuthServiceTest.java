package in.fixna.platform.auth;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import in.fixna.platform.auth.dto.LoginRequest;
import in.fixna.platform.auth.dto.RegisterRequest;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.TenantMembershipRepository;
import in.fixna.platform.tenant.TenantRepository;
import in.fixna.platform.user.User;
import in.fixna.platform.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Service-level auth tests (no DB): duplicate registration, bad credentials,
 * and refresh rotation/replay rules.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock TenantRepository tenants;
    @Mock TenantMembershipRepository memberships;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwt;
    @Mock AuditPublisher audit;

    JwtProperties jwtProperties = new JwtProperties(
            "test-secret-that-is-long-enough-for-hs256-minimum-32bytes!!",
            java.time.Duration.ofMinutes(15),
            java.time.Duration.ofDays(7));

    AuthService service() {
        return new AuthService(
                users, tenants, memberships, refreshTokens, passwordEncoder, jwt, jwtProperties, audit);
    }

    @Test
    void duplicateEmailRejected() {
        when(users.existsByEmail("a@x.in")).thenReturn(true);

        assertThatThrownBy(() -> service().register(
                        new RegisterRequest("a@x.in", "password123", null, null, "Tenant A")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void unknownEmailRejectedWithGenericMessage() {
        when(users.findByEmail("ghost@x.in")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().login(new LoginRequest("ghost@x.in", "whatever123")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void wrongPasswordRejected() {
        User user = new User();
        user.setEmail("a@x.in");
        user.setPasswordHash("hash");
        when(users.findByEmail("a@x.in")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service().login(new LoginRequest("a@x.in", "bad-password")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void refreshWithUnknownHashRejected() {
        JwtService realJwt = new JwtService(jwtProperties);
        UUID userId = UUID.randomUUID();
        String raw = realJwt.issueRefresh(userId, UUID.randomUUID());
        when(refreshTokens.findByTokenHashAndRevokedFalse(RefreshToken.hash(raw)))
                .thenReturn(Optional.empty());

        AuthService svc = new AuthService(
                users, tenants, memberships, refreshTokens, passwordEncoder, realJwt, jwtProperties, audit);

        assertThatThrownBy(() -> svc.refresh(raw))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesAllTokens() {
        AuthService svc = service();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(refreshTokens.revokeAllForUser(userId)).thenReturn(2);

        svc.logout(userId, tenantId);

        org.mockito.Mockito.verify(refreshTokens).revokeAllForUser(userId);
        org.mockito.Mockito.verify(audit).publish(any());
    }
}
