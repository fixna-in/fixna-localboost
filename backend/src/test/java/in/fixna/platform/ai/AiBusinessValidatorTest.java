package in.fixna.platform.ai;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Request-side business rules (BR-3/BR-4 shadow checks) before provider call. */
class AiBusinessValidatorTest {

    private final AiBusinessValidator validator = new AiBusinessValidator();

    @Test
    void negativeBudgetRejected() {
        assertThatThrownBy(() -> validator.validate(
                        RecommendationType.CAMPAIGN_STRATEGY, Map.of("budget", -1)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_VALIDATION_FAILED");
    }

    @Test
    void allocationsAboveBudgetRejected() {
        assertThatThrownBy(() -> validator.validate(
                        RecommendationType.BUDGET_ALLOCATION,
                        Map.of("budget", 100,
                                "channels", List.of(
                                        Map.of("channel", "META", "amount", 70),
                                        Map.of("channel", "GOOGLE", "amount", 50)))))
                .isInstanceOf(FixnaException.class)
                .hasMessageContaining("BR-4");
    }

    @Test
    void duplicateChannelsRejected() {
        assertThatThrownBy(() -> validator.validate(
                        RecommendationType.BUDGET_ALLOCATION,
                        Map.of("budget", 100,
                                "channels", List.of(
                                        Map.of("channel", "META", "amount", 50),
                                        Map.of("channel", "META", "amount", 40)))))
                .isInstanceOf(FixnaException.class);
    }

    @Test
    void validPayloadPasses() {
        assertThatCode(() -> validator.validate(
                        RecommendationType.BUDGET_ALLOCATION,
                        Map.of("budget", 100,
                                "channels", List.of(
                                        Map.of("channel", "META", "amount", 60),
                                        Map.of("channel", "GOOGLE", "amount", 40)))))
                .doesNotThrowAnyException();
    }

    @Test
    void audienceAgeBoundsEnforced() {
        assertThatThrownBy(() -> validator.validate(
                        RecommendationType.AUDIENCE, Map.of("ageMin", 10)))
                .isInstanceOf(FixnaException.class);
    }

    @Test
    void outputAudienceAgeBoundsEnforced() {
        assertThatThrownBy(() -> validator.validateOutput(
                        RecommendationType.AUDIENCE, Map.of("ageMax", 90)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_OUTPUT_BUSINESS_INVALID");
    }

    @Test
    void outputDuplicateAllocationChannelsRejected() {
        assertThatThrownBy(() -> validator.validateOutput(
                        RecommendationType.BUDGET_ALLOCATION,
                        Map.of("allocations", List.of(
                                Map.of("channel", "META", "amount", 50),
                                Map.of("channel", "META", "amount", 40)))))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_OUTPUT_BUSINESS_INVALID");
    }

    @Test
    void outputValidBudgetAllocationPasses() {
        assertThatCode(() -> validator.validateOutput(
                        RecommendationType.BUDGET_ALLOCATION,
                        Map.of("allocations", List.of(
                                Map.of("channel", "META", "amount", 60),
                                Map.of("channel", "GOOGLE", "amount", 40)))))
                .doesNotThrowAnyException();
    }
}
