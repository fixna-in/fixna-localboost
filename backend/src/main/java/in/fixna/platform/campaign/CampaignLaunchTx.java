package in.fixna.platform.campaign;

import java.util.UUID;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.LaunchReceipt;

/**
 * Transactional state edges of launch execution, isolated into their own
 * bean so the coordinator ({@link CampaignLaunchService}) can run external
 * adapter calls OUTSIDE any database transaction (backend rule 04).
 * begin(): QUEUED/CREATING → CREATING. applyOutcome(): CREATING → ACTIVE | FAILED
 * with audit. Both are tenant-scoped and no-op safely if state moved on.
 */
@Service
public class CampaignLaunchTx {

    private final CampaignRepository campaigns;
    private final AuditPublisher audit;

    public CampaignLaunchTx(CampaignRepository campaigns, AuditPublisher audit) {
        this.campaigns = campaigns;
        this.audit = audit;
    }

    @Transactional
    public void begin(UUID campaignId) {
        TenantContext.requireWrite();
        Campaign campaign = campaigns
                .findByIdAndTenantId(campaignId, TenantContext.requireTenantId())
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
        CampaignStatus status = campaign.getStatus();
        if (status != CampaignStatus.QUEUED && status != CampaignStatus.CREATING) {
            throw new FixnaException(
                    "CAMPAIGN_NOT_IN_QUEUE", HttpStatus.CONFLICT,
                    "Launch execution must start from QUEUED (current: " + status + ")");
        }
        campaign.setStatus(CampaignStatus.CREATING);
        campaigns.save(campaign);
        audit.publish(new AuditEvent(
                "campaign.creating", campaign.getTenantId(), TenantContext.requireUserId(),
                "campaign", campaign.getId().toString(), Map.of(), null));
    }

    /**
     * Applies the outcome. Idempotent guard: only a CREATING campaign is
     * moved; if state advanced elsewhere this returns without change.
     */
    @Transactional
    public void applyOutcome(UUID campaignId, List<LaunchReceipt> receipts, String errorCode, String errorMessage) {
        Campaign campaign = campaigns
                .findByIdAndTenantId(campaignId, TenantContext.requireTenantId())
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
        if (campaign.getStatus() != CampaignStatus.CREATING) {
            return;
        }
        if (errorCode == null) {
            campaign.setStatus(CampaignStatus.ACTIVE);
            campaigns.save(campaign);
            audit.publish(new AuditEvent(
                    "campaign.launch_succeeded", campaign.getTenantId(), TenantContext.requireUserId(),
                    "campaign", campaign.getId().toString(),
                    Map.of(
                            "platforms", joined(receipts, LaunchReceipt::platform),
                            "externalIds", joined(receipts, LaunchReceipt::externalCampaignId)),
                    null));
        } else {
            campaign.setStatus(CampaignStatus.FAILED);
            campaigns.save(campaign);
            audit.publish(new AuditEvent(
                    "campaign.launch_failed", campaign.getTenantId(), TenantContext.requireUserId(),
                    "campaign", campaign.getId().toString(),
                    Map.of(
                            "code", errorCode,
                            "error", truncate(errorMessage)),
                    null));
        }
    }

    private String joined(List<LaunchReceipt> receipts, java.util.function.Function<LaunchReceipt, String> f) {
        return receipts.stream().map(f).collect(java.util.stream.Collectors.joining(","));
    }

    private String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
