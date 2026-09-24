package in.fixna.platform.common.logging;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Guards logs against secrets and unnecessary PII.
 *
 * <p>Rule: never log the sensitive values at all. This helper is for (a)
 * detecting sensitive keys before emitting maps/details and (b) masking rare
 * diagnostic values (for example the tail of an external id) when a log line
 * genuinely needs them. WhatsApp/lead message content, credentials, tokens,
 * passwords, authorization headers, cookies, API keys and webhook secrets are
 * never logged.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "secret", "clientsecret", "client_secret",
            "accesstoken", "access_token", "refreshtoken", "refresh_token",
            "apikey", "api_key", "x-api-key", "authorization", "cookie",
            "set-cookie", "setcookie", "jwt", "privatekey", "private_key",
            "webhooksecret", "webhook_secret", "databasepassword", "database_password",
            "token", "sessiontoken", "session_token");

    /** Returns true when the key must never appear in a log line. */
    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        return SENSITIVE_KEYS.contains(normalized);
    }

    /**
     * Masks all but the last 4 characters (for rare diagnostics).
     * Returns {@code "***"} for short/null values.
     */
    public static String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        return "******" + value.substring(value.length() - 4);
    }

    /**
     * Safe identifier for auth-failure logs: never the raw email. Uses a
     * stable truncated SHA-256 hex so repeated failures correlate without
     * exposing the address.
     */
    public static String userIdentifierHash(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            return Integer.toHexString(normalized.hashCode());
        }
    }

    /** Masks an IPv4/IPv6 client address to a /24-ish diagnostic form. */
    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "***";
        }
        // Reuse the existing rate-limit masking shape: keep prefix, hide host.
        int lastDot = ip.lastIndexOf('.');
        int lastColon = ip.lastIndexOf(':');
        if (lastDot >= 0 && (lastColon < 0 || lastDot > lastColon)) {
            return ip.substring(0, lastDot + 1) + "***";
        }
        if (lastColon >= 0) {
            return ip.substring(0, lastColon + 1) + "***";
        }
        return "***";
    }

    /** Generates the safe external-operation reference used in adapter logs. */
    public static String safeReference(UUID id) {
        return id == null ? null : id.toString();
    }
}
