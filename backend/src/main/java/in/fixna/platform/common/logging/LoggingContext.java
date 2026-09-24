package in.fixna.platform.common.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;

import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;

/**
 * Central helper for MDC-based contextual logging.
 *
 * <p>Populates {@code requestId} (always), plus {@code traceId}/{@code spanId}
 * when a tracer provides them, and {@code tenantId}/{@code userId} from the
 * JWT-derived {@link TenantContext} only — never from client input.
 * {@code campaignId} and {@code operation} are set per business flow by
 * services/controllers and cleared by the same owner.
 */
public final class LoggingContext {

    private LoggingContext() {}

    public static void putRequestId(String requestId) {
        putIfPresent(LoggingConstants.REQUEST_ID, requestId);
    }

    /** Copies OTel trace/span ids into MDC; null-safe, no tracing system created. */
    public static void putTrace(String traceId, String spanId) {
        putIfPresent(LoggingConstants.TRACE_ID, traceId);
        putIfPresent(LoggingConstants.SPAN_ID, spanId);
    }

    /** Populates tenant/user MDC from the authenticated context when present. */
    public static void putTenantAndUser() {
        UUID tenant = tenantOrNull();
        UUID user = userOrNull();
        putIfPresent(LoggingConstants.TENANT_ID, tenant == null ? null : tenant.toString());
        putIfPresent(LoggingConstants.USER_ID, user == null ? null : user.toString());
    }

    public static void putTenantAndUser(UUID tenantId, UUID userId) {
        putIfPresent(LoggingConstants.TENANT_ID, tenantId == null ? null : tenantId.toString());
        putIfPresent(LoggingConstants.USER_ID, userId == null ? null : userId.toString());
    }

    public static void putCampaignId(UUID campaignId) {
        putIfPresent(LoggingConstants.CAMPAIGN_ID, campaignId == null ? null : campaignId.toString());
    }

    public static void putCampaignId(String campaignId) {
        putIfPresent(LoggingConstants.CAMPAIGN_ID, campaignId);
    }

    public static void putOperation(String operation) {
        putIfPresent(LoggingConstants.OPERATION, operation);
    }

    /** Snapshot for async propagation; restore with {@link #restore(Map)}. */
    public static Map<String, String> snapshot() {
        Map<String, String> copy = MDC.getCopyOfContextMap();
        return copy == null ? Map.of() : new HashMap<>(copy);
    }

    /** Restores a snapshot captured on the request thread (async workers). */
    public static void restore(Map<String, String> snapshot) {
        MDC.clear();
        if (snapshot != null && !snapshot.isEmpty()) {
            MDC.setContextMap(snapshot);
        }
    }

    /** Clears request-scoped correlation keys (requestId/tenant/user/campaign/operation/trace). */
    public static void clearRequest() {
        MDC.remove(LoggingConstants.TRACE_ID);
        MDC.remove(LoggingConstants.SPAN_ID);
        MDC.remove(LoggingConstants.REQUEST_ID);
        MDC.remove(LoggingConstants.TENANT_ID);
        MDC.remove(LoggingConstants.USER_ID);
        MDC.remove(LoggingConstants.CAMPAIGN_ID);
        MDC.remove(LoggingConstants.OPERATION);
    }

    private static void putIfPresent(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }

    /** Null-safe scope lookup: logging must never fail because scope is absent. */
    private static UUID tenantOrNull() {
        try {
            return TenantContext.requireTenantId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static UUID userOrNull() {
        try {
            return TenantContext.requireUserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
