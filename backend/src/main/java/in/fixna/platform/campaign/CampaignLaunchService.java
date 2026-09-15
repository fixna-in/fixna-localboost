package in.fixna.platform.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import in.fixna.platform.campaign.dto.CampaignResponse;
import in.fixna.platform.common.observability.OperationTimer;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.notification.NotificationProvider.Notification;
import in.fixna.platform.notification.NotificationService;
import in.fixna.platform.platform.AdvertisingPlatformAdapter;
import in.fixna.platform.platform.LaunchReceipt;
import in.fixna.platform.platform.PlatformAdapterRegistry;
import in.fixna.platform.platform.PlatformException;
import in.fixna.platform.platform.PlatformLaunchRequest;

/**
 * Launch execution coordinator (BR-5/BR-6, ADC-005). Deliberately NOT
 * transactional: it commits the CREATING edge, performs the (mock) external
 * adapter calls with no open transaction, bounded-retries only retryable
 * failures, then commits the ACTIVE/FAILED edge. Idempotent: a campaign not
 * in QUEUED/CREATING is returned unchanged with zero adapter calls; adapter
 * external ids derive from external_reference so re-runs never duplicate.
 */
@Service
public class CampaignLaunchService {

    static final int MAX_ATTEMPTS = 3;

    private final CampaignRepository campaigns;
    private final CampaignChannelRepository channels;
    private final PlatformAdapterRegistry registry;
    private final CampaignLaunchTx tx;
    private final NotificationService notifications;

    public CampaignLaunchService(
            CampaignRepository campaigns,
            CampaignChannelRepository channels,
            PlatformAdapterRegistry registry,
            CampaignLaunchTx tx,
            NotificationService notifications) {
        this.campaigns = campaigns;
        this.channels = channels;
        this.registry = registry;
        this.tx = tx;
        this.notifications = notifications;
    }

    /** Executes QUEUED → CREATING → ACTIVE | FAILED for the caller's tenant. */
    public CampaignResponse run(UUID campaignId) {
        UUID tenantId = TenantContext.requireTenantId();
        Campaign campaign = campaigns
                .findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
        switch (campaign.getStatus()) {
            case QUEUED, CREATING -> { /* proceed below */ }
            case ACTIVE, PAUSED -> {
                return CampaignResponse.from(campaign); // idempotent no-op
            }
            default -> throw new FixnaException(
                    "CAMPAIGN_NOT_IN_QUEUE", HttpStatus.CONFLICT,
                    "Launch execution must start from QUEUED (current: " + campaign.getStatus() + ")");
        }

        tx.begin(campaignId);

        List<CampaignChannel> allocations =
                channels.findByTenantIdAndCampaignId(tenantId, campaignId);
        List<LaunchReceipt> receipts = new ArrayList<>();
        PlatformException failure = null;
        for (CampaignChannel allocation : allocations) {
            try {
                AdvertisingPlatformAdapter adapter = registry.forChannel(allocation.getChannel());
                PlatformLaunchRequest request = new PlatformLaunchRequest(
                        tenantId,
                        campaignId,
                        campaign.getExternalReference(),
                        campaign.getName(),
                        campaign.getObjective() == null ? null : campaign.getObjective().name(),
                        allocation.getChannel(),
                        allocation.getAllocatedBudget(),
                        campaign.getCurrency(),
                        campaign.getStartAt(),
                        campaign.getEndAt(),
                        null);
                Attempt attempt = attempt(adapter, request);
                if (attempt.failure() == null) {
                    receipts.add(attempt.receipt());
                } else {
                    failure = attempt.failure();
                    break;
                }
            } catch (PlatformException ex) {
                // Unknown channel / adapter resolution failure must also land in
                // the FAILED outcome — never escape and strand the campaign
                // in CREATING.
                failure = ex;
                break;
            }
        }

        tx.applyOutcome(
                campaignId,
                receipts,
                failure == null ? null : resolveCode(failure),
                failure == null ? null : failure.getMessage());

        Campaign fresh = campaigns
                .findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
        String event = fresh.getStatus() == CampaignStatus.ACTIVE ? "campaign.launched" : "campaign.launch_failed";
        notifications.publish(new Notification(
                event, tenantId, "campaign", campaignId.toString(),
                Map.of("status", fresh.getStatus().name())));
        return CampaignResponse.from(fresh);
    }

    /** Outcome of one channel's launch attempt: exactly one side is non-null. */
    private record Attempt(LaunchReceipt receipt, PlatformException failure) {}

    /**
     * Bounded retries for retryable failures only; returns the last outcome.
     * Each attempt emits one {@code platform.launch} telemetry line (WF11):
     * adapter, campaign, durationMs and per-attempt status. Never logs the
     * request payload or any token.
     */
    private Attempt attempt(AdvertisingPlatformAdapter adapter, PlatformLaunchRequest request) {
        PlatformException last = null;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            OperationTimer timer = OperationTimer.start(
                    "platform.launch", "campaign", request.campaignId().toString(), adapter.platform());
            try {
                LaunchReceipt receipt = adapter.launch(request);
                timer.status("SUCCESS");
                return new Attempt(receipt, null);
            } catch (PlatformException ex) {
                timer.status("FAILED");
                last = ex;
                if (!ex.isRetryable()) {
                    return new Attempt(null, last);
                }
            } finally {
                timer.close();
            }
        }
        return new Attempt(null, last);
    }

    private String resolveCode(PlatformException failure) {
        return failure.isRetryable() ? "PROVIDER_RETRY_EXHAUSTED" : failure.getCode();
    }
}
