package in.fixna.platform.common.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit fact for security-relevant operations (auth events,
 * campaign approvals/launches, membership changes, ...).
 *
 * <p>Never carries secrets, tokens or passwords — {@link AuditPublisher}
 * implementations must treat the details map as untrusted for logging.
 */
public record AuditEvent(
        String action,
        UUID tenantId,
        UUID actorUserId,
        String entityType,
        String entityId,
        Map<String, String> details,
        OffsetDateTime occurredAt) {

    public AuditEvent {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Audit action is required");
        }
        details = details == null ? Map.of() : Map.copyOf(details);
        occurredAt = occurredAt == null ? OffsetDateTime.now() : occurredAt;
    }
}
