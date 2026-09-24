package in.fixna.platform.common.logging;

/**
 * MDC keys, operation names and service identity for structured logging.
 *
 * <p>MDC keys are the single source of truth for correlation fields used by
 * {@code log4j2-spring.xml} and JSON consumers. Values are populated via
 * {@link LoggingContext} (never from client input for tenant/user).
 */
public final class LoggingConstants {

    private LoggingConstants() {}

    /** Microservice identifier emitted as {@code service} in JSON logs. */
    public static final String SERVICE_NAME = "fixna-localboost-backend";

    // MDC keys (also JsonLayout properties).
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String CAMPAIGN_ID = "campaignId";
    public static final String OPERATION = "operation";

    // Incoming correlation header (trusted propagation only sanitizes format).
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    // Consistent operation names for important business flows.
    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String CAMPAIGN_CREATE = "CAMPAIGN_CREATE";
    public static final String CAMPAIGN_UPDATE = "CAMPAIGN_UPDATE";
    public static final String CAMPAIGN_APPROVE = "CAMPAIGN_APPROVE";
    public static final String CAMPAIGN_LAUNCH = "CAMPAIGN_LAUNCH";
    public static final String CAMPAIGN_PAUSE = "CAMPAIGN_PAUSE";
    public static final String CAMPAIGN_COMPLETE = "CAMPAIGN_COMPLETE";
    public static final String PLATFORM_CREATE_CAMPAIGN = "PLATFORM_CREATE_CAMPAIGN";
    public static final String PLATFORM_UPDATE_CAMPAIGN = "PLATFORM_UPDATE_CAMPAIGN";
    public static final String PLATFORM_PAUSE_CAMPAIGN = "PLATFORM_PAUSE_CAMPAIGN";
    public static final String AI_RECOMMENDATION_GENERATE = "AI_RECOMMENDATION_GENERATE";
    public static final String LEAD_CREATE = "LEAD_CREATE";
    public static final String LEAD_UPDATE = "LEAD_UPDATE";
}
