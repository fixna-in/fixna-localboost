package in.fixna.platform.ai;

import java.util.List;
import java.util.Map;

/**
 * Deterministic mock provider for local/demo mode (ADR-010). Produces
 * schema-shaped payloads per recommendation type without any external call.
 * The same request always yields an equal result (record equality), so demos
 * and tests are reproducible.
 */
public class MockAIProvider implements AIProvider {

    public static final String PROMPT_VERSION = "mock-v1";

    private static final double DEFAULT_STRATEGY_BUDGET = 5000d;
    private static final double DEFAULT_ALLOCATION_BUDGET = 1000d;

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String model() {
        return "mock-model-1";
    }

    @Override
    public RecommendationResult generate(RecommendationRequest request) {
        Map<String, Object> data = switch (request.type()) {
            case CAMPAIGN_STRATEGY -> strategyData(request);
            case AUDIENCE -> audienceData();
            case BUDGET_ALLOCATION -> budgetData(request);
            case CREATIVE -> creativeData();
        };
        // Deterministic fake usage so cost tracking is exercised end to end.
        return new RecommendationResult(
                name(), model(), PROMPT_VERSION, data, 120, 180, 400L);
    }

    private Map<String, Object> strategyData(RecommendationRequest request) {
        return Map.of(
                "objective", "FOOTFALL",
                "channels", List.of("META", "GOOGLE"),
                "recommendedBudget", positiveBudget(request.context().get("budget"), DEFAULT_STRATEGY_BUDGET),
                "rationale", "Local visibility play for a new SMB in Delhi NCR.");
    }

    private Map<String, Object> audienceData() {
        return Map.of(
                "name", "Nearby high-intent customers",
                "ageMin", 22,
                "ageMax", 45,
                "radiusKm", 8);
    }

    /** Splits the requested budget 60/40 so the allocation sums exactly. */
    private Map<String, Object> budgetData(RecommendationRequest request) {
        double budget = positiveBudget(request.context().get("budget"), DEFAULT_ALLOCATION_BUDGET);
        double googleShare = Math.round(budget * 0.6d);
        return Map.of(
                "allocations", List.of(
                        Map.of("channel", "GOOGLE", "amount", googleShare),
                        Map.of("channel", "META", "amount", budget - googleShare)));
    }

    private Map<String, Object> creativeData() {
        return Map.of(
                "headline", "Taste Noida this weekend",
                "description", "Walk in today and save.",
                "cta", "GET_OFFER");
    }

    private double positiveBudget(Object value, double fallback) {
        if (value instanceof Number number && number.doubleValue() > 0) {
            return number.doubleValue();
        }
        return fallback;
    }
}
