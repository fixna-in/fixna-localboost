package in.fixna.platform.platform.dto;

import java.time.OffsetDateTime;

import in.fixna.platform.platform.PlatformConnection;

/**
 * Connection response. Deliberately excludes the encrypted token columns —
 * credentials never leave the server.
 */
public record PlatformConnectionResponse(
        String platform,
        String externalAccountId,
        String status,
        OffsetDateTime createdAt) {

    public static PlatformConnectionResponse from(PlatformConnection connection) {
        return new PlatformConnectionResponse(
                connection.getPlatform(),
                connection.getExternalAccountId(),
                connection.getStatus(),
                connection.getCreatedAt());
    }
}
