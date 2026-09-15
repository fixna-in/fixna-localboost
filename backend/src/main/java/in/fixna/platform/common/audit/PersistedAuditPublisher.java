package in.fixna.platform.common.audit;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link AuditPublisher} that also persists to {@code audit_logs} (WF09).
 * Persistence happens after commit so audit writes never roll back — or block
 * — the originating business transaction; failures are logged and swallowed.
 */
@Component
public class PersistedAuditPublisher implements AuditPublisher {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.audit");

    private final AuditViewerService viewer;
    private final LoggingAuditPublisher logging;

    public PersistedAuditPublisher(AuditViewerService viewer, LoggingAuditPublisher logging) {
        this.viewer = viewer;
        this.logging = logging;
    }

    @Override
    public void publish(AuditEvent event) {
        logging.publish(event);
        Runnable write = () -> {
            try {
                viewer.record(
                        event.tenantId(),
                        event.actorUserId(),
                        event.action(),
                        event.entityType(),
                        event.entityId(),
                        event.details() == null ? Map.of() : event.details(),
                        event.occurredAt());
            } catch (RuntimeException ex) {
                LOG.warn("Audit persist failed for action {}", event.action());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write.run();
                }
            });
        } else {
            write.run();
        }
    }
}
