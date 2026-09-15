package in.fixna.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Refresh request carrying the opaque refresh token (raw value, once). */
public record RefreshRequest(@NotBlank String refreshToken) {}
