package in.fixna.platform.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Connect payload. Tokens are never accepted from the client in mock mode. */
public record PlatformConnectionRequest(
        @NotBlank @Size(max = 30) String platform,
        @Size(max = 255) String externalAccountId) {}
