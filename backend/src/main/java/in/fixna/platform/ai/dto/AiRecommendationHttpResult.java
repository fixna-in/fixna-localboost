package in.fixna.platform.ai.dto;

import java.util.Map;

import in.fixna.platform.ai.RecommendationResult;

/** AI recommendation HTTP response: validated data plus usage metadata. */
public record AiRecommendationHttpResult(
        String provider,
        String model,
        String promptVersion,
        Map<String, Object> data,
        int promptTokens,
        int completionTokens,
        long estimatedCostMicros) {

    public static AiRecommendationHttpResult from(RecommendationResult result) {
        return new AiRecommendationHttpResult(
                result.provider(),
                result.model(),
                result.promptVersion(),
                result.data(),
                result.promptTokens(),
                result.completionTokens(),
                result.estimatedCostMicros());
    }
}
