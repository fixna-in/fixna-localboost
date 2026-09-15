package in.fixna.platform.creative;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.creative.dto.CreativeRequest;
import in.fixna.platform.creative.dto.CreativeResponse;

/**
 * US-011: tenant-scoped creative drafts. Every creative is bound to an owned
 * campaign whose tenant is verified before any read/write. AI suggestions are
 * advisory; content is only stored here after user review (rules 11/13).
 */
@Service
public class CreativeService {

    private final CreativeRepository creatives;
    private final CampaignRepository campaigns;
    private final AuditPublisher audit;

    public CreativeService(
            CreativeRepository creatives, CampaignRepository campaigns, AuditPublisher audit) {
        this.creatives = creatives;
        this.campaigns = campaigns;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CreativeResponse> list(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        Campaign campaign = requireOwnedCampaign(campaignId);
        return creatives.findByTenantIdAndCampaignId(tenantId, campaign.getId()).stream()
                .map(CreativeResponse::from)
                .toList();
    }

    /** Creates a DRAFT creative; status is always DRAFT on creation. */
    @Transactional
    public CreativeResponse create(UUID campaignId, CreativeRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        validate(request);
        Creative creative = new Creative();
        creative.setTenantId(campaign.getTenantId());
        creative.setCampaignId(campaign.getId());
        apply(creative, request);
        creative.setStatus(CreativeStatus.DRAFT);
        creatives.save(creative);
        audit.publish(new AuditEvent(
                "creative.created", campaign.getTenantId(), TenantContext.requireUserId(),
                "creative", creative.getId().toString(),
                Map.of("campaign", campaign.getId().toString(),
                        "channel", creative.getChannel()), null));
        return CreativeResponse.from(creative);
    }

    @Transactional
    public CreativeResponse update(UUID campaignId, UUID creativeId, CreativeRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwnedCampaign(campaignId);
        validate(request);
        Creative creative = requireOwned(creativeId);
        if (!campaign.getId().equals(creative.getCampaignId())) {
            throw new FixnaException(
                    "CREATIVE_MISMATCH", HttpStatus.BAD_REQUEST,
                    "Creative does not belong to this campaign");
        }
        CreativeStatus target = request.status() == null ? creative.getStatus() : request.status();
        if (creative.getStatus() != target && !creative.getStatus().canTransitionTo(target)) {
            throw new FixnaException(
                    "ILLEGAL_CREATIVE_STATUS", HttpStatus.CONFLICT,
                    "Cannot set creative status from " + creative.getStatus() + " to " + target);
        }
        apply(creative, request);
        creative.setStatus(target);
        creatives.save(creative);
        audit.publish(new AuditEvent(
                "creative.updated", campaign.getTenantId(), TenantContext.requireUserId(),
                "creative", creative.getId().toString(),
                Map.of("status", creative.getStatus().name()), null));
        return CreativeResponse.from(creative);
    }

    @Transactional
    public void delete(UUID campaignId, UUID creativeId) {
        TenantContext.requireWrite();
        requireOwnedCampaign(campaignId);
        Creative creative = requireOwned(creativeId);
        if (!campaignId.equals(creative.getCampaignId())) {
            throw new FixnaException(
                    "CREATIVE_MISMATCH", HttpStatus.BAD_REQUEST,
                    "Creative does not belong to this campaign");
        }
        creatives.delete(creative);
        audit.publish(new AuditEvent(
                "creative.deleted", creative.getTenantId(), TenantContext.requireUserId(),
                "creative", creative.getId().toString(), Map.of(), null));
    }

    /** Content rule: channel required plus at least one copy field non-blank. */
    private void validate(CreativeRequest request) {
        if (request.channel() == null || request.channel().isBlank()) {
            throw invalid("channel is required");
        }
        boolean hasCopy = hasText(request.headline())
                || hasText(request.body())
                || hasText(request.callToAction());
        if (!hasCopy) {
            throw invalid("at least one of headline, body or callToAction is required");
        }
    }

    private void apply(Creative creative, CreativeRequest request) {
        creative.setChannel(request.channel().trim().toUpperCase());
        creative.setHeadline(trimToNull(request.headline()));
        creative.setBody(trimToNull(request.body()));
        creative.setCallToAction(trimToNull(request.callToAction()));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private FixnaException invalid(String detail) {
        return new FixnaException(
                "INVALID_CREATIVE", HttpStatus.BAD_REQUEST, "Invalid creative: " + detail);
    }

    /** Tenant-scoped campaign lookup — cross-tenant ids surface as NOT_FOUND. */
    private Campaign requireOwnedCampaign(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        return campaigns
                .findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    /** Tenant-scoped creative lookup. */
    private Creative requireOwned(UUID creativeId) {
        UUID tenantId = TenantContext.requireTenantId();
        return creatives
                .findByIdAndTenantId(creativeId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CREATIVE_NOT_FOUND", HttpStatus.NOT_FOUND, "Creative not found"));
    }
}