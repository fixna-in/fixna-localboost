package in.fixna.platform.common.observability;

import java.util.UUID;

import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import org.slf4j.MDC;

/**
 * Correlation fields for structured telemetry (rule 14). Resolved from the
 * request where available; never from client input or payload values.
 * Absent values serialize as {@code null}.
 */
public final class TelemetryContext {

    private TelemetryContext() {}

    /** Request id from the RequestIdFilter MDC slot, if present. */
    public static String requestId() {
        String value = MDC.get(in.fixna.platform.common.web.RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value == null || value.isBlank() ? null : value;
    }

    /** Current tenant id from the JWT-derived TenantContext, if present. */
    public static UUID tenantId() {
        try {
            return TenantContext.requireTenantId();
        } catch (FixnaException ex) {
            return null;
        }
    }

    /** Current user id from the JWT-derived TenantContext, if present. */
    public static UUID userId() {
        try {
            return TenantContext.requireUserId();
        } catch (FixnaException ex) {
            return null;
        }
    }
}