package in.fixna.platform.common.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.fixna.platform.common.tenant.TenantContext;

/**
 * Lightweight OpenTelemetry-ready operation hook: times a block and emits one
 * structured line to {@code fixna.telemetry} on close. Correlation fields
 * (requestId/tenantId/userId) come from the request context, plus the
 * caller-supplied operation/entity ids. A future workflow can swap this with
 * a real OpenTelemetry tracer without touching call sites.
 *
 * <pre>{@code
 *   try (OperationTimer t = timers.start("campaign.launch", "campaign", campaignId)) {
 *       adapter.launch(request);
 *       t.status("SUCCESS");
 *   }
 * }</pre>
 *
 * Never logs payloads, secrets, tokens or passwords — only operation ids and
 * duration.
 */
public final class OperationTimer implements AutoCloseable {

    private static final Logger TELEMETRY_LOG = LoggerFactory.getLogger("fixna.telemetry");

    private final String operation;
    private final String entityType;
    private final String entityId;
    private final String platform;
    private final Instant startedAt;

    private String status = "OK";
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private OperationTimer(String operation, String entityType, String entityId, String platform) {
        this.operation = operation;
        this.entityType = entityType;
        this.entityId = entityId;
        this.platform = platform;
        this.startedAt = Instant.now();
    }

    /** Opens a timer; block completes when {@code close()} is called. */
    public static OperationTimer start(String operation, String entityType, String entityId) {
        return start(operation, entityType, entityId, null);
    }

    /** Opens a timer for a platform/external operation (e.g. an adapter call). */
    public static OperationTimer start(String operation, String entityType, String entityId, String platform) {
        return new OperationTimer(operation, entityType, entityId, platform);
    }

    /** Marks the operation's final state before close (default OK). */
    public OperationTimer status(String status) {
        this.status = status;
        return this;
    }

    @Override
    public void close() {
        // Idempotent: try-with-resources or defensive double-close emits once.
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        String requestId = TelemetryContext.requestId();
        UUID tenantId = lookupTenantId();
        UUID userId = TelemetryContext.userId();
        TELEMETRY_LOG.info(
                "operation={} status={} durationMs={} requestId={} tenantId={} userId={}"
                        + " entity={}:{} platform={}",
                operation, status, durationMs, requestId, tenantId, userId,
                entityType, entityId, platform);
    }

    private static UUID lookupTenantId() {
        try {
            return TenantContext.requireTenantId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}