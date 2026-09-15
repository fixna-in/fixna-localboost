package in.fixna.platform.ai.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** AI recommendation HTTP request. Tenant scope always comes from the JWT. */
public record AiRecommendationHttpRequest(
        @NotNull RecommendationTypeRef type,
        UUID businessId,
        UUID campaignId,
        Map<String, Object> payload) {

    /** Indirection keeps the DTO decoupled from enum import order issues. */
    public enum RecommendationTypeRef {
        CAMPAIGN_STRATEGY, AUDIENCE, BUDGET_ALLOCATION, CREATIVE
    }
}
