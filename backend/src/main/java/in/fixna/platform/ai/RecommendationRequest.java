package in.fixna.platform.ai;

import java.util.Map;
import java.util.UUID;

/**
 * Provider-agnostic request for an AI recommendation. Carries the caller's
 * tenant/business/campaign context (resolved server-side, never from client
 * tenant claims) plus a free-form business context map.
 */
public record RecommendationRequest(
        UUID tenantId,
        UUID businessId,
        UUID campaignId,
        RecommendationType type,
        Map<String, Object> context) {

    /** Factory with a non-null context; arguments in (type, ids..., context) order. */
    public static RecommendationRequest of(
            RecommendationType type,
            UUID tenantId,
            UUID businessId,
            UUID campaignId,
            Map<String, Object> context) {
        return new RecommendationRequest(
                tenantId, businessId, campaignId, type,
                context == null ? Map.of() : context);
    }
}
