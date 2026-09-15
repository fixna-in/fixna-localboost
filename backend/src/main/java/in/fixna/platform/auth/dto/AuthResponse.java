package in.fixna.platform.auth.dto;

import java.util.UUID;

/** Token pair returned by register/login/refresh. Raw refresh shown once. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        UUID tenantId,
        String role) {

    public static AuthResponse bearer(
            String accessToken, String refreshToken, long expiresInSeconds,
            UUID userId, UUID tenantId, String role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, userId, tenantId, role);
    }
}
