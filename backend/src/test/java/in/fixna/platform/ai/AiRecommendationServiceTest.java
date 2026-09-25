package in.fixna.platform.ai;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.MockGoogleAdsAdapter;
import in.fixna.platform.platform.MockMetaAdsAdapter;
import in.fixna.platform.platform.MockWhatsAppAdapter;
import in.fixna.platform.platform.PlatformAdapterRegistry;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Service pipeline: validate -> provider -> schema -> usage + audit. */
@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    @Mock AIProvider provider;
    @Mock AiQuotaChecker quotaChecker;
    @Mock AiUsageRepository usageRepository;
    @Mock AuditPublisher audit;

    private AiRecommendationService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AiPlatformCompatibilityValidator platformCompatibilityValidator = new AiPlatformCompatibilityValidator(
                new PlatformAdapterRegistry(java.util.List.of(
                        new MockGoogleAdsAdapter(), new MockMetaAdsAdapter(), new MockWhatsAppAdapter())));
        service = new AiRecommendationService(
                provider, new AiSchemaValidator(), new AiBusinessValidator(),
                platformCompatibilityValidator, quotaChecker, usageRepository, audit);
        TenantContext.set(tenantId, userId, MembershipRole.TENANT_OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void validFlowRecordsUsageAndAudit() {
        when(provider.generate(any())).thenReturn(new RecommendationResult(
                "mock", "m", "v1",
                Map.of("objective", "FOOTFALL",
                        "channels", java.util.List.of("META"),
                        "recommendedBudget", 100,
                        "rationale", "nearby reach"),
                10, 20, 5L));

        RecommendationResult result = service.recommend(
                RecommendationType.CAMPAIGN_STRATEGY, null, UUID.randomUUID(), Map.of("budget", 100));

        assertThat(result.provider()).isEqualTo("mock");
        verify(usageRepository).save(any(AiUsage.class));
        verify(audit).publish(any());
    }

    @Test
    void schemaInvalidResultRejectedWithoutUsage() {
        when(provider.generate(any())).thenReturn(new RecommendationResult(
                "mock", "m", "v1", Map.of(), 1, 1, 1L));

        assertThatThrownBy(() -> service.recommend(
                        RecommendationType.CAMPAIGN_STRATEGY, null, null, Map.of()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_RESPONSE_INVALID");
        verify(usageRepository, never()).save(any(AiUsage.class));
    }

    @Test
    void requestPayloadBusinessRulesRunBeforeProvider() {
        assertThatThrownBy(() -> service.recommend(
                        RecommendationType.BUDGET_ALLOCATION, null, null,
                        Map.of("budget", -5)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_VALIDATION_FAILED");
        verify(provider, never()).generate(any());
        verify(usageRepository, never()).save(any(AiUsage.class));
    }

    @Test
    void unsupportedPlatformRejectedWithoutUsage() {
        when(provider.generate(any())).thenReturn(new RecommendationResult(
                "mock", "m", "v1",
                Map.of("objective", "FOOTFALL",
                        "channels", java.util.List.of("TIKTOK"),
                        "recommendedBudget", 100,
                        "rationale", "x"),
                1, 1, 1L));

        assertThatThrownBy(() -> service.recommend(
                        RecommendationType.CAMPAIGN_STRATEGY, null, null, Map.of()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_PLATFORM_INCOMPATIBLE");
        verify(usageRepository, never()).save(any(AiUsage.class));
    }
}
