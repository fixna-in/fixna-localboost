package in.fixna.platform.common.audit;

/**
 * Abstraction over audit persistence. The MVP implementation logs; a future
 * workflow persists to {@code audit_logs} without touching call sites.
 */
public interface AuditPublisher {

    void publish(AuditEvent event);
}
