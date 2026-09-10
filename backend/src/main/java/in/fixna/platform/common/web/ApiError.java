package in.fixna.platform.common.web;

import java.time.OffsetDateTime;

/**
 * Standard API error envelope for all externally visible failures.
 *
 * <p>Shape: timestamp, status, code, message, path, requestId. Never carries
 * secrets, tokens or stack traces.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId) {}
