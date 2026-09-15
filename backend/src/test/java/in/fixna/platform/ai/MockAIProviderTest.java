package in.fixna.platform.ai;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Mock provider determinism and per-type payload shape. */
class MockAIProviderTest {

    private final MockAIProvider provider = new MockAIProvider();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    @Test
    void strategyIsDeterministicAndShaped() {
        RecommendationRequest request = RecommendationRequest.of(
                RecommendationType.CAMPAIGN_STRATEGY, tenantId, null, campaignId, Map.of("budget", 8000));

        RecommendationResult first = provider.generate(request);
        RecommendationResult second = provider.generate(request);

        assertThat(first).isEqualTo(second);
        assertThat(first.data()).containsKeys("objective", "channels", "recommendedBudget", "rationale");
        assertThat(first.promptVersion()).isEqualTo("mock-v1");
        assertThat(first.estimatedCostMicros()).isPositive();
    }

    @Test
    void budgetAllocationRespectsRequestedBudget() {
        RecommendationResult result = provider.generate(RecommendationRequest.of(
                RecommendationType.BUDGET_ALLOCATION, tenantId, null, campaignId, Map.of("budget", 1000)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allocations = (List<Map<String, Object>>) result.data().get("allocations");
        double sum = allocations.stream()
                .mapToDouble(a -> ((Number) a.get("amount")).doubleValue())
                .sum();
        assertThat(sum).isEqualTo(1000d);
    }

    @Test
    void creativeHasBoundedHeadlineAndKnownCta() {
        RecommendationResult result = provider.generate(RecommendationRequest.of(
                RecommendationType.CREATIVE, tenantId, null, null, Map.of()));

        assertThat((String) result.data().get("headline")).hasSizeLessThanOrEqualTo(60);
        assertThat((String) result.data().get("cta")).isEqualTo("GET_OFFER");
    }
}
