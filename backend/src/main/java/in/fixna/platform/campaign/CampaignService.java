package in.fixna.platform.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.billing.PlanLimitChecker;
import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.dto.CampaignOfferRequest;
import in.fixna.platform.campaign.dto.CampaignOfferResponse;
import in.fixna.platform.campaign.dto.CampaignRequest;
import in.fixna.platform.campaign.dto.CampaignResponse;
import in.fixna.platform.campaign.dto.ChannelAllocation;
import in.fixna.platform.campaign.dto.ChannelAllocationRequest;
import in.fixna.platform.campaign.dto.ChannelAllocationResponse;
import in.fixna.platform.campaign.dto.TransitionRequest;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.logging.LoggingConstants;
import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;

/**
 * Campaign application service, part 1: CRUD + lifecycle transitions.
 * Enforces BR-1 (exactly one tenant + one owned business), BR-3 (positive
 * budget), BR-5 (launch only after approval), BR-6 (idempotent launch).
 */
@Service
public class CampaignService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaigns;
    private final CampaignOfferRepository offers;
    private final CampaignChannelRepository channels;
    private final BusinessRepository businesses;
    private final AuditPublisher audit;
    private final PlanLimitChecker planLimits;

    public CampaignService(
            CampaignRepository campaigns,
            CampaignOfferRepository offers,
            CampaignChannelRepository channels,
            BusinessRepository businesses,
            AuditPublisher audit,
            PlanLimitChecker planLimits) {
        this.campaigns = campaigns;
        this.offers = offers;
        this.channels = channels;
        this.businesses = businesses;
        this.audit = audit;
        this.planLimits = planLimits;
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> list(UUID businessId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<Campaign> result = businessId == null
                ? campaigns.findByTenantId(tenantId)
                : campaigns.findByTenantIdAndBusinessId(
                        tenantId, requireOwnedBusiness(businessId).getId());
        return result.stream().map(CampaignResponse::from).toList();
    }

    @Transactional
    public CampaignResponse create(CampaignRequest request) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        planLimits.checkCampaignCreate();
        var business = requireOwnedBusiness(request.businessId());
        validateWindow(request.startAt(), request.endAt());
        Campaign campaign = new Campaign();
        campaign.setTenantId(tenantId);
        campaign.setBusinessId(business.getId());
        apply(campaign, request);
        campaigns.save(campaign);
        LoggingContext.putOperation(LoggingConstants.CAMPAIGN_CREATE);
        LoggingContext.putCampaignId(campaign.getId());
        LOG.info("Campaign created successfully campaignId={} tenantId={}", campaign.getId(), tenantId);
        audit.publish(new AuditEvent(
                "campaign.created", tenantId, TenantContext.requireUserId(), "campaign",
                campaign.getId().toString(), Map.of("business", business.getId().toString()), null));
        return CampaignResponse.from(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(UUID id) {
        return CampaignResponse.from(requireOwned(id));
    }

    /** Edits core fields. Allowed only in DRAFT (brief editable pre-review). */
    @Transactional
    public CampaignResponse update(UUID id, CampaignRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwned(id);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new FixnaException(
                    "CAMPAIGN_LOCKED", HttpStatus.CONFLICT,
                    "Campaign can only be edited in DRAFT status");
        }
        var business = requireOwnedBusiness(request.businessId());
        validateWindow(request.startAt(), request.endAt());
        campaign.setBusinessId(business.getId());
        apply(campaign, request);
        campaigns.save(campaign);
        LOG.info("Campaign updated campaignId={} status={}", campaign.getId(), campaign.getStatus());
        audit.publish(new AuditEvent(
                "campaign.updated", campaign.getTenantId(), TenantContext.requireUserId(), "campaign",
                campaign.getId().toString(), Map.of(), null));
        return CampaignResponse.from(campaign);
    }

    @Transactional
    public void delete(UUID id) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwned(id);
        if (campaign.getStatus().isLaunchable() || campaign.getStatus() == CampaignStatus.ACTIVE) {
            throw new FixnaException(
                    "CAMPAIGN_LOCKED", HttpStatus.CONFLICT,
                    "Approved or active campaigns cannot be deleted");
        }
        offers.findByTenantIdAndCampaignId(campaign.getTenantId(), campaign.getId())
                .forEach(offers::delete);
        channels.findByTenantIdAndCampaignId(campaign.getTenantId(), campaign.getId())
                .forEach(channels::delete);
        campaigns.delete(campaign);
        audit.publish(new AuditEvent(
                "campaign.deleted", campaign.getTenantId(), TenantContext.requireUserId(), "campaign",
                campaign.getId().toString(), Map.of(), null));
    }

    /**
     * Explicit lifecycle transition (approval path). Validates against
     * {@link CampaignStatus#canTransitionTo}; launch itself goes through
     * {@link #launch} so idempotency semantics stay in one place.
     */
    @Transactional
    public CampaignResponse transition(UUID id, TransitionRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwned(id);
        CampaignStatus target = request.to();
        if (campaign.getStatus() == target) {
            return CampaignResponse.from(campaign);
        }
        if (!campaign.getStatus().canTransitionTo(target)
                || target == CampaignStatus.QUEUED
                || target == CampaignStatus.CREATING) {
            throw new FixnaException(
                    "ILLEGAL_TRANSITION", HttpStatus.CONFLICT,
                    "Cannot transition campaign from " + campaign.getStatus() + " to " + target);
        }
        CampaignStatus from = campaign.getStatus();
        campaign.setStatus(target);
        campaigns.save(campaign);
        LoggingContext.putCampaignId(campaign.getId());
        LoggingContext.putOperation(operationForTransition(target));
        LOG.info("Campaign transitioned campaignId={} from={} to={}", campaign.getId(), from, target);
        audit.publish(new AuditEvent(
                "campaign.transitioned", campaign.getTenantId(), TenantContext.requireUserId(),
                "campaign", campaign.getId().toString(),
                Map.of("from", from.name(), "to", target.name()), null));
        return CampaignResponse.from(campaign);
    }

    /**
     * Idempotent launch orchestration (BR-5/BR-6). Safe entry states are
     * APPROVED (first launch) and FAILED (retry). Repeating the call on an
     * already-queued-or-later campaign returns current state instead of
     * duplicating side effects — external_reference is the idempotency key
     * handed to adapters in Workflow 07 (executed outside the DB txn).
     */
    @Transactional
    public CampaignResponse launch(UUID id) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwned(id);
        switch (campaign.getStatus()) {
            case APPROVED, FAILED -> {
                campaign.setStatus(CampaignStatus.QUEUED);
                campaigns.save(campaign);
                LoggingContext.putOperation(LoggingConstants.CAMPAIGN_LAUNCH);
                LoggingContext.putCampaignId(campaign.getId());
                LOG.info("Campaign launch queued campaignId={} externalReference={}",
                        campaign.getId(), campaign.getExternalReference());
                audit.publish(new AuditEvent(
                        "campaign.launch_requested", campaign.getTenantId(), TenantContext.requireUserId(),
                        "campaign", campaign.getId().toString(),
                        Map.of("externalReference", campaign.getExternalReference()), null));
                return CampaignResponse.from(campaign);
            }
            case QUEUED, CREATING, ACTIVE, PAUSED -> {
                LOG.debug("Campaign launch already in progress campaignId={} status={}",
                        campaign.getId(), campaign.getStatus());
                return CampaignResponse.from(campaign);
            }
            default -> {
                LOG.warn("Campaign launch rejected campaignId={} status={}", campaign.getId(), campaign.getStatus());
                throw new FixnaException(
                        "NOT_APPROVED", HttpStatus.CONFLICT,
                        "Campaign must be APPROVED before launch (current: " + campaign.getStatus() + ")");
            }
        }
    }

    @Transactional
    public in.fixna.platform.campaign.dto.CampaignOfferResponse addOffer(
            UUID campaignId, in.fixna.platform.campaign.dto.CampaignOfferRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwned(campaignId);
        CampaignOffer offer = new CampaignOffer();
        offer.setTenantId(campaign.getTenantId());
        offer.setCampaignId(campaign.getId());
        offer.setTitle(request.title().trim());
        offer.setDescription(request.description());
        offer.setPromoCode(request.promoCode());
        offers.save(offer);
        audit.publish(new AuditEvent(
                "campaign.offer_added", campaign.getTenantId(), TenantContext.requireUserId(),
                "campaign_offer", offer.getId().toString(),
                Map.of("campaign", campaign.getId().toString()), null));
        return in.fixna.platform.campaign.dto.CampaignOfferResponse.from(offer);
    }

    @Transactional(readOnly = true)
    public List<in.fixna.platform.campaign.dto.CampaignOfferResponse> listOffers(UUID campaignId) {
        Campaign campaign = requireOwned(campaignId);
        return offers.findByTenantIdAndCampaignId(campaign.getTenantId(), campaign.getId()).stream()
                .map(in.fixna.platform.campaign.dto.CampaignOfferResponse::from)
                .toList();
    }

    /**
     * Replace-all channel budget split. BR-4: sum of allocations must not
     * exceed the campaign total. Duplicate channel names are rejected.
     */
    @Transactional
    public List<in.fixna.platform.campaign.dto.ChannelAllocationResponse> replaceChannels(
            UUID campaignId, in.fixna.platform.campaign.dto.ChannelAllocationRequest request) {
        TenantContext.requireWrite();
        Campaign campaign = requireOwned(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT
                && campaign.getStatus() != CampaignStatus.READY_FOR_REVIEW) {
            throw new FixnaException(
                    "CAMPAIGN_LOCKED", HttpStatus.CONFLICT,
                    "Channel budgets can only change before approval");
        }
        var allocations = request.channels();
        long distinct = allocations.stream()
                .map(a -> a.channel().trim().toUpperCase()).distinct().count();
        if (distinct != allocations.size()) {
            throw new FixnaException(
                    "DUPLICATE_CHANNEL", HttpStatus.BAD_REQUEST, "Each channel may appear only once");
        }
        java.math.BigDecimal sum = allocations.stream()
                .map(in.fixna.platform.campaign.dto.ChannelAllocation::allocatedBudget)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (sum.compareTo(campaign.getTotalBudget()) > 0) {
            throw new FixnaException(
                    "BUDGET_EXCEEDED", HttpStatus.BAD_REQUEST,
                    "Channel allocations (" + sum + ") exceed total budget (" + campaign.getTotalBudget()
                            + ")");
        }
        channels.findByTenantIdAndCampaignId(campaign.getTenantId(), campaign.getId())
                .forEach(channels::delete);
        channels.flush();
        List<CampaignChannel> saved = allocations.stream()
                .map(a -> {
                    CampaignChannel channel = new CampaignChannel();
                    channel.setTenantId(campaign.getTenantId());
                    channel.setCampaignId(campaign.getId());
                    channel.setChannel(a.channel().trim().toUpperCase());
                    channel.setAllocatedBudget(a.allocatedBudget());
                    return channels.save(channel);
                })
                .toList();
        audit.publish(new AuditEvent(
                "campaign.channels_updated", campaign.getTenantId(), TenantContext.requireUserId(),
                "campaign", campaign.getId().toString(), Map.of("allocated", sum.toPlainString()), null));
        return saved.stream()
                .map(in.fixna.platform.campaign.dto.ChannelAllocationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChannelAllocationResponse> listChannels(UUID campaignId) {
        Campaign campaign = requireOwned(campaignId);
        return channels.findByTenantIdAndCampaignId(campaign.getTenantId(), campaign.getId()).stream()
                .map(ChannelAllocationResponse::from)
                .toList();
    }

    private void apply(Campaign campaign, CampaignRequest request) {
        campaign.setName(request.name().trim());
        campaign.setObjective(request.objective());
        campaign.setTotalBudget(request.totalBudget());
        campaign.setCurrency(request.currency() == null ? "INR" : request.currency().toUpperCase());
        campaign.setStartAt(request.startAt());
        campaign.setEndAt(request.endAt());
    }

    private void validateWindow(java.time.OffsetDateTime start, java.time.OffsetDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new FixnaException(
                    "INVALID_WINDOW", HttpStatus.BAD_REQUEST, "Campaign end must be after start");
        }
    }

    /** Tenant-scoped fetch — cross-tenant ids surface as NOT_FOUND (no leakage). */
    private Campaign requireOwned(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return campaigns
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    /** BR-1: the parent business must belong to the caller's tenant. */
    private in.fixna.platform.business.Business requireOwnedBusiness(UUID businessId) {
        UUID tenantId = TenantContext.requireTenantId();
        return businesses
                .findByIdAndTenantId(businessId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "BUSINESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Business not found"));
    }

    /** Stable operation name for transition logs (approve/pause/complete fallbacks). */
    private static String operationForTransition(CampaignStatus target) {
        return switch (target) {
            case APPROVED -> LoggingConstants.CAMPAIGN_APPROVE;
            case PAUSED -> LoggingConstants.CAMPAIGN_PAUSE;
            case COMPLETED -> LoggingConstants.CAMPAIGN_COMPLETE;
            default -> LoggingConstants.CAMPAIGN_UPDATE;
        };
    }
}
