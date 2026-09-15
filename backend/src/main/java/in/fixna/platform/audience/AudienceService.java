package in.fixna.platform.audience;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audience.dto.AudienceRequest;
import in.fixna.platform.audience.dto.AudienceResponse;
import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;

/**
 * US-008: tenant-scoped audience definition. The audience JSON is validated
 * structurally (recognized criteria keys, numeric ranges, non-empty lists)
 * before storage — AI output rule: anything stored is validated, never just
 * trusted.
 */
@Service
public class AudienceService {

    /** Recognized audience criteria; unknown keys are rejected to catch typos. */
    static final Set<String> KNOWN_CRITERIA = Set.of(
            "ageMin", "ageMax", "genders", "interests", "incomeBrackets", "deviceTypes", "languages");

    private static final Set<String> STRING_LIST_CRITERIA = Set.of(
            "genders", "interests", "incomeBrackets", "deviceTypes", "languages");

    private final AudienceRepository audiences;
    private final CampaignRepository campaigns;
    private final AuditPublisher audit;

    public AudienceService(
            AudienceRepository audiences, CampaignRepository campaigns, AuditPublisher audit) {
        this.audiences = audiences;
        this.campaigns = campaigns;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AudienceResponse> list(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        Campaign campaign = requireOwnedCampaign(campaignId);
        return audiences.findByTenantIdAndCampaignId(tenantId, campaign.getId()).stream()
                .map(AudienceResponse::from)
                .toList();
    }

    @Transactional
    public AudienceResponse create(UUID campaignId, AudienceRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        validate(request.definition());
        Audience audience = new Audience();
        audience.setTenantId(campaign.getTenantId());
        audience.setCampaignId(campaign.getId());
        audience.setName(request.name().trim());
        audience.setDefinition(request.definition());
        audiences.save(audience);
        audit.publish(new AuditEvent(
                "audience.created", campaign.getTenantId(), TenantContext.requireUserId(),
                "audience", audience.getId().toString(),
                Map.of("campaign", campaign.getId().toString()), null));
        return AudienceResponse.from(audience);
    }

    @Transactional
    public AudienceResponse update(UUID campaignId, UUID audienceId, AudienceRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        validate(request.definition());
        Audience audience = requireOwned(audienceId);
        if (!campaign.getId().equals(audience.getCampaignId())) {
            throw new FixnaException(
                    "AUDIENCE_MISMATCH", HttpStatus.BAD_REQUEST,
                    "Audience does not belong to this campaign");
        }
        audience.setName(request.name().trim());
        audience.setDefinition(request.definition());
        audiences.save(audience);
        audit.publish(new AuditEvent(
                "audience.updated", campaign.getTenantId(), TenantContext.requireUserId(),
                "audience", audience.getId().toString(), Map.of(), null));
        return AudienceResponse.from(audience);
    }

    @Transactional
    public void delete(UUID campaignId, UUID audienceId) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        Audience audience = requireOwned(audienceId);
        if (!campaign.getId().equals(audience.getCampaignId())) {
            throw new FixnaException(
                    "AUDIENCE_MISMATCH", HttpStatus.BAD_REQUEST,
                    "Audience does not belong to this campaign");
        }
        audiences.delete(audience);
        audit.publish(new AuditEvent(
                "audience.deleted", campaign.getTenantId(), TenantContext.requireUserId(),
                "audience", audience.getId().toString(), Map.of(), null));
    }

    /** Structural validation of the audience definition JSON. */
    private void validate(Map<String, Object> definition) {
        for (Map.Entry<String, Object> entry : definition.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!KNOWN_CRITERIA.contains(key)) {
                throw invalid("Unknown criterion '" + key + "'");
            }
            if ("ageMin".equals(key) || "ageMax".equals(key)) {
                if (!(value instanceof Number)) {
                    throw invalid(key + " must be a number");
                }
            } else if (STRING_LIST_CRITERIA.contains(key)) {
                if (!(value instanceof List<?> list) || list.isEmpty()) {
                    throw invalid(key + " must be a non-empty string array");
                }
                if (list.stream().anyMatch(v -> !(v instanceof String) || ((String) v).isBlank())) {
                    throw invalid(key + " must contain only non-blank strings");
                }
            }
        }
        Number ageMin = (Number) definition.get("ageMin");
        Number ageMax = (Number) definition.get("ageMax");
        if (ageMin != null && ageMax != null && ageMin.doubleValue() > ageMax.doubleValue()) {
            throw invalid("ageMin must not exceed ageMax");
        }
        if (ageMin != null && ageMin.doubleValue() < 0) {
            throw invalid("ageMin cannot be negative");
        }
    }

    private FixnaException invalid(String detail) {
        return new FixnaException(
                "INVALID_AUDIENCE", HttpStatus.BAD_REQUEST, "Invalid audience: " + detail);
    }

    /** Tenant-scoped campaign lookup — cross-tenant ids surface as NOT_FOUND. */
    private Campaign requireOwnedCampaign(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        return campaigns
                .findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    /** Tenant-scoped audience lookup. */
    private Audience requireOwned(UUID audienceId) {
        UUID tenantId = TenantContext.requireTenantId();
        return audiences
                .findByIdAndTenantId(audienceId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "AUDIENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Audience not found"));
    }
}