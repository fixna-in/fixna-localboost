package in.fixna.platform.ai;

import java.util.Map;

/**
 * Structured AI recommendation output plus usage metadata (rule 13).
 * AI is advisory: callers must obtain user approval before any execution —
 * providers can never mutate campaign state, budgets or launch anything.
 *
 * @param provider            provider name (e.g. "mock")
 * @param model               model identifier
 * @param promptVersion       versioned prompt identifier
 * @param data                payload for the recommendation type (untrusted until validated)
 * @param promptTokens        prompt tokens reported by the provider
 * @param completionTokens    completion tokens reported by the provider
 * @param estimatedCostMicros estimated cost in micros of USD
 */
public record RecommendationResult(
        String provider,
        String model,
        String promptVersion,
        Map<String, Object> data,
        int promptTokens,
        int completionTokens,
        long estimatedCostMicros) {}
