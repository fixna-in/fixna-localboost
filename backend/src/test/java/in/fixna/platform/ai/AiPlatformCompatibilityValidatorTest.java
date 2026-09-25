package in.fixna.platform.ai;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.MockGoogleAdsAdapter;
import in.fixna.platform.platform.MockMetaAdsAdapter;
import in.fixna.platform.platform.MockWhatsAppAdapter;
import in.fixna.platform.platform.PlatformAdapterRegistry;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Platform references in AI output must resolve to registered adapters. */
class AiPlatformCompatibilityValidatorTest {

    private final AiPlatformCompatibilityValidator validator = new AiPlatformCompatibilityValidator(
            new PlatformAdapterRegistry(List.of(
                    new MockGoogleAdsAdapter(), new MockMetaAdsAdapter(), new MockWhatsAppAdapter())));

    @Test
    void supportedStrategyChannelsPass() {
        assertThatCode(() -> validator.validate(RecommendationType.CAMPAIGN_STRATEGY, Map.of(
                "channels", List.of("META", "GOOGLE"))))
                .doesNotThrowAnyException();
    }

    @Test
    void channelAliasesResolve() {
        assertThatCode(() -> validator.validate(RecommendationType.CAMPAIGN_STRATEGY, Map.of(
                "channels", List.of("GOOGLE_ADS", "META_ADS"))))
                .doesNotThrowAnyException();
    }

    @Test
    void unsupportedStrategyChannelRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.CAMPAIGN_STRATEGY, Map.of(
                "channels", List.of("TIKTOK"))))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_PLATFORM_INCOMPATIBLE");
    }

    @Test
    void unsupportedBudgetAllocationChannelRejected() {
        assertThatThrownBy(() -> validator.validate(RecommendationType.BUDGET_ALLOCATION, Map.of(
                "allocations", List.of(
                        Map.of("channel", "PINTEREST", "amount", 100)))))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_PLATFORM_INCOMPATIBLE");
    }

    @Test
    void audienceAndCreativeSkipPlatformCheck() {
        assertThatCode(() -> validator.validate(RecommendationType.AUDIENCE, Map.of(
                "name", "Nearby", "ageMin", 22, "ageMax", 45)))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(RecommendationType.CREATIVE, Map.of(
                "headline", "ok", "description", "ok", "cta", "GET_OFFER")))
                .doesNotThrowAnyException();
    }
}
