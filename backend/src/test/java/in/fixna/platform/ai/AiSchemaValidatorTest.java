package in.fixna.platform.ai;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Structural rejection of untrusted AI output per recommendation type. */
class AiSchemaValidatorTest {

    private final AiSchemaValidator validator = new AiSchemaValidator();

    @Test
    void validStrategyPasses() {
        assertThatCode(() -> validator.validate(RecommendationType.CAMPAIGN_STRATEGY, Map.of(
                "objective", "FOOTFALL",
                "channels", List.of("META", "GOOGLE"),
                "recommendedBudget", 5000,
                "rationale", "Nearby reach")))
                .doesNotThrowAnyException();
    }

    @Test
    void emptyPayloadRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.CAMPAIGN_STRATEGY, Map.of()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_RESPONSE_INVALID");
    }

    @Test
    void blankChannelRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.CAMPAIGN_STRATEGY, Map.of(
                "objective", "FOOTFALL",
                "channels", List.of(" "),
                "recommendedBudget", 100,
                "rationale", "x")))
                .isInstanceOf(FixnaException.class);
    }

    @Test
    void longCreativeHeadlineRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.CREATIVE, Map.of(
                "headline", "x".repeat(61),
                "description", "ok",
                "cta", "GET_OFFER")))
                .isInstanceOf(FixnaException.class);
    }

    @Test
    void unknownCtaRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.CREATIVE, Map.of(
                "headline", "ok",
                "description", "ok",
                "cta", "BUY_NOW")))
                .isInstanceOf(FixnaException.class);
    }

    @Test
    void invertedAgesRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.AUDIENCE, Map.of(
                "name", "Nearby", "ageMin", 45, "ageMax", 22)))
                .isInstanceOf(FixnaException.class);
    }
}
