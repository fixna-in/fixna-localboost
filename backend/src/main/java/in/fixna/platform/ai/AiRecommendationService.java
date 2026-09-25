package in.fixna.platform.ai;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.logging.LoggingConstants;
import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.observability.OperationTimer;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;

/**
 * AI recommendation application service (flow per rules 11/13):
 * input business rules -> quota -> provider -> schema validation ->
 * output business rules -> platform compatibility -> usage/audit ->
 * recommendation returned for USER APPROVAL.
 * AI is advisory: this service never mutates campaign state, budgets or
 * launch status, and never touches credentials.
 */
@Service
public class AiRecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(AiRecommendationService.class);

    private final AIProvider provider;
    private final AiSchemaValidator schemaValidator;
    private final AiBusinessValidator businessValidator;
    private final AiPlatformCompatibilityValidator platformCompatibilityValidator;
    private final AiQuotaChecker quotaChecker;
    private final AiUsageRepository usageRepository;
    private final AuditPublisher audit;

    public AiRecommendationService(
            AIProvider provider,
            AiSchemaValidator schemaValidator,
            AiBusinessValidator businessValidator,
            AiPlatformCompatibilityValidator platformCompatibilityValidator,
            AiQuotaChecker quotaChecker,
            AiUsageRepository usageRepository,
            AuditPublisher audit) {
        this.provider = provider;
        this.schemaValidator = schemaValidator;
        this.businessValidator = businessValidator;
        this.platformCompatibilityValidator = platformCompatibilityValidator;
        this.quotaChecker = quotaChecker;
        this.usageRepository = usageRepository;
        this.audit = audit;
    }

    /**
     * Runs the validated recommendation flow. Tenant/user scope comes from
     * {@link TenantContext} (JWT), never from client parameters.
     */
    public RecommendationResult recommend(
            RecommendationType type, UUID businessId, UUID campaignId, Map<String, Object> payload) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        // Request rules first: malformed business input never reaches a provider.
        businessValidator.validate(type, payload);
        // Tenant quota gate (rule 11 / WF09): counts before the provider call.
        quotaChecker.checkCurrentTenant();
        RecommendationRequest request = RecommendationRequest.of(
                type, tenantId, businessId, campaignId, payload);
        RecommendationResult result;
        long startedAt = System.nanoTime();
        try (OperationTimer timer = OperationTimer.start(
                "ai.recommend", "campaign",
                campaignId == null ? null : campaignId.toString(),
                type.name())) {
            try {
                result = provider.generate(request);
                timer.status("SUCCESS");
            } catch (FixnaException ex) {
                timer.status("PROVIDER_ERROR");
                throw ex;
            } catch (RuntimeException ex) {
                timer.status("PROVIDER_ERROR");
                throw new FixnaException(
                        "AI_PROVIDER_FAILED", HttpStatus.BAD_GATEWAY, "AI provider call failed", ex);
            }
        }
        // Output is untrusted until validated; no usage row on failure.
        schemaValidator.validate(type, result.data());
        businessValidator.validateOutput(type, result.data());
        platformCompatibilityValidator.validate(type, result.data());
        recordUsage(tenantId, userId, type, result, (System.nanoTime() - startedAt) / 1_000_000L);
        LoggingContext.putOperation(LoggingConstants.AI_RECOMMENDATION_GENERATE);
        LoggingContext.putCampaignId(campaignId);
        LOG.info("AI recommendation generated type={} provider={} model={} tenantId={} campaignId={}"
                        + " inputTokens={} outputTokens={} durationMs={}",
                type, result.provider(), result.model(), tenantId, campaignId,
                result.promptTokens(), result.completionTokens(),
                (System.nanoTime() - startedAt) / 1_000_000L);
        audit.publish(new AuditEvent(
                "ai.recommendation_generated", tenantId, userId, "ai_recommendation",
                campaignId == null ? null : campaignId.toString(),
                Map.of("type", type.name(), "provider", result.provider()),
                null));
        return result;
    }

    private void recordUsage(
            UUID tenantId, UUID userId, RecommendationType type, RecommendationResult result,
            long durationMs) {
        try {
            AiUsage usage = new AiUsage();
            usage.setTenantId(tenantId);
            usage.setUserId(userId);
            usage.setRecommendationType(type.name());
            usage.setProvider(result.provider());
            usage.setModel(result.model());
            usage.setPromptVersion(result.promptVersion());
            usage.setInputTokens(result.promptTokens());
            usage.setOutputTokens(result.completionTokens());
            usage.setEstimatedCostUsd(BigDecimal.valueOf(result.estimatedCostMicros(), 6));
            usage.setDurationMs(durationMs);
            usage.setStatus("SUCCESS");
            usageRepository.save(usage);
        } catch (RuntimeException ignored) {
            // Usage recording must never fail or mask the recommendation flow.
        }
    }
}
