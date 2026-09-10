package in.fixna.platform.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MVP {@link AuditPublisher} writing structured single-line audit facts to
 * the log. Carries only ids/action metadata — never secrets or tokens.
 */
@Component
public class LoggingAuditPublisher implements AuditPublisher {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("fixna.audit");

    @Override
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
