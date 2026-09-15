package in.fixna.platform.notification;

import java.util.Map;
import java.util.UUID;

/**
 * Outbound notification abstraction (WF09). Implementations deliver or log;
 * domain services depend only on this interface. Payload details must never
 * contain secrets, tokens, passwords, or raw customer PII beyond ids.
 */
public interface NotificationProvider {

    /** Channel this provider serves (for example MOCK, LOG, WHATSAPP). */
    String channel();

    /** Sends a tenant-scoped notification event. */
    void send(Notification notification);

    /** Immutable outbound event with tenant context and string-only details. */
    record Notification(String event, UUID tenantId, String entityType, String entityId,
            Map<String, String> details) {

        public Notification {
            if (event == null || event.isBlank()) {
                throw new IllegalArgumentException("Notification event is required");
            }
            details = details == null ? Map.of() : Map.copyOf(details);
        }
    }
}
