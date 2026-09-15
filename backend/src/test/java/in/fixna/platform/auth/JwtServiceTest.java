package in.fixna.platform.auth;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for JWT issue/verify: type separation and tamper rejection. */
class JwtServiceTest {

    private final JwtProperties properties = new JwtProperties(
            "test-secret-that-is-long-enough-for-hs256-minimum-32bytes!!",
            Duration.ofMinutes(15),
            Duration.ofDays(7));
    private final JwtService jwt = new JwtService(properties);

    @Test
    void accessTokenRoundTripsTenantAndRole() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        java.util.UUID tenantId = java.util.UUID.randomUUID();

        String token = jwt.issueAccess(userId, tenantId, "TENANT_OWNER");

        JwtService.VerifiedToken verified = jwt.verify(token, "access");
        assertThat(verified.userId()).isEqualTo(userId);
        assertThat(verified.tenantId()).isEqualTo(tenantId);
        assertThat(verified.role()).isEqualTo("TENANT_OWNER");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccess() {
        String refresh = jwt.issueRefresh(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());

        assertThatThrownBy(() -> jwt.verify(refresh, "access"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwt.issueAccess(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "TENANT_VIEWER");
        String tampered = token.substring(0, token.length() - 2) + "ab";

        assertThatThrownBy(() -> jwt.verify(tampered, "access"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
