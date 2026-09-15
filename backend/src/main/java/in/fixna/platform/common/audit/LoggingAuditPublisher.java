package in.fixna.platform.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MVP audit sink writing structured single-line audit facts to
 * the log. Carries only ids/action metadata — never secrets or tokens.
 * Delegated to by {@link PersistedAuditPublisher} (the primary
 * {@link AuditPublisher}); kept as a separate bean for log-only use.
 */
@Component
public class LoggingAuditPublisher {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("fixna.audit");

    /** Writes the structured single-line audit fact (no persistence here). */
    public void publish(AuditEvent event) {
        AUDIT_LOG.info(
                "action={} tenant={} actor={} entity={}:{} details={} at={}",
                event.action(),
                event.tenantId(),
                event.actorUserId(),
                event.entityType(),
                event.entityId(),
                event.details(),
                event.occurredAt());
    }
}
