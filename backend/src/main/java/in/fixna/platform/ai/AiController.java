package in.fixna.platform.ai;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.ai.dto.AiRecommendationHttpRequest;
import in.fixna.platform.ai.dto.AiRecommendationHttpResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AI recommendation endpoints. Controllers translate HTTP only; tenant
 * scope comes from the JWT and AI stays advisory (no state mutation).
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "ai", description = "Advisory AI recommendations with usage tracking")
public class AiController {

    private final AiRecommendationService recommendationService;

    public AiController(AiRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "Generate a validated AI recommendation")
    @PostMapping("/recommendations")
    public ResponseEntity<AiRecommendationHttpResult> recommend(
            @Valid @RequestBody AiRecommendationHttpRequest request) {
        RecommendationResult result = recommendationService.recommend(
                RecommendationType.valueOf(request.type().name()),
                request.businessId(),
                request.campaignId(),
                request.payload());
        return ResponseEntity.ok(AiRecommendationHttpResult.from(result));
    }
}
