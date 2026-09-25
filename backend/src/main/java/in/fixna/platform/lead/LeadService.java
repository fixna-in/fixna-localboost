package in.fixna.platform.lead;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.logging.LoggingConstants;
import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.lead.dto.LeadPageResponse;
import in.fixna.platform.lead.dto.LeadRequest;
import in.fixna.platform.lead.dto.LeadResponse;
import in.fixna.platform.lead.dto.LeadStatusRequest;
import in.fixna.platform.notification.NotificationProvider.Notification;
import in.fixna.platform.notification.NotificationService;

/**
 * Lead application service (US funnel: capture → nurture → convert).
 * Writes verify parent business ownership (and optional campaign ownership);
 * reads are tenant-scoped and paginated. Terminal funnel stages are locked.
 */
@Service
public class LeadService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadService.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final LeadRepository leads;
    private final BusinessRepository businesses;
    private final CampaignRepository campaigns;
    private final AuditPublisher audit;
    private final NotificationService notifications;

    public LeadService(
            LeadRepository leads,
            BusinessRepository businesses,
            CampaignRepository campaigns,
            AuditPublisher audit,
            NotificationService notifications) {
        this.leads = leads;
        this.businesses = businesses;
        this.campaigns = campaigns;
        this.audit = audit;
        this.notifications = notifications;
    }

    @Transactional
    public LeadResponse create(UUID businessId, LeadRequest request) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        if (businessId == null || businesses.findByIdAndTenantId(businessId, tenantId).isEmpty()) {
            throw new FixnaException(
                    "BUSINESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Business not found");
        }
        UUID campaignId = request.campaignId();
        if (campaignId != null && campaigns.findByIdAndTenantId(campaignId, tenantId).isEmpty()) {
            throw new FixnaException(
                    "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found");
        }
        Lead lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setBusinessId(businessId);
        lead.setCampaignId(campaignId);
        lead.setName(request.name());
        lead.setPhone(request.phone());
        lead.setEmail(request.email());
        lead.setStatus(LeadStatus.NEW);
        lead.setSource(request.source());
        leads.save(lead);

        LoggingContext.putOperation(LoggingConstants.LEAD_CREATE);
        LoggingContext.putCampaignId(campaignId);
        LOG.debug("Lead captured leadId={} businessId={} campaignId={} tenantId={}",
                lead.getId(), businessId, campaignId, tenantId);
        audit.publish(new AuditEvent(
                "lead.created", tenantId, TenantContext.requireUserId(), "lead",
                lead.getId().toString(), Map.of("business", businessId.toString()), null));
        notifications.publish(new Notification(
                "lead.created", tenantId, "lead", lead.getId().toString(),
                Map.of("business", businessId.toString())));
        return LeadResponse.from(lead);
    }

    @Transactional(readOnly = true)
    public LeadPageResponse list(UUID businessId, UUID campaignId, LeadStatus status, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Lead> result = leads.search(tenantId, businessId, campaignId, status, pageable);
        return LeadPageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(UUID id) {
        return LeadResponse.from(requireOwned(id));
    }

    /** Funnel transition; terminal stages (CONVERTED/LOST) cannot change again. */
    @Transactional
    public LeadResponse updateStatus(UUID id, LeadStatusRequest request) {
        TenantContext.requireWrite();
        Lead lead = requireOwned(id);
        if (lead.getStatus().isTerminal()) {
            throw new FixnaException(
                    "LEAD_TERMINAL", HttpStatus.CONFLICT,
                    "Lead is in terminal state " + lead.getStatus() + " and cannot change");
        }
        lead.setStatus(request.status());
        leads.save(lead);
        audit.publish(new AuditEvent(
                "lead.status_updated", lead.getTenantId(), TenantContext.requireUserId(), "lead",
                lead.getId().toString(), Map.of("status", request.status().name()), null));
        return LeadResponse.from(lead);
    }

    @Transactional
    public void delete(UUID id) {
        TenantContext.requireWrite();
        Lead lead = requireOwned(id);
        leads.delete(lead);
        audit.publish(new AuditEvent(
                "lead.deleted", lead.getTenantId(), TenantContext.requireUserId(), "lead",
                lead.getId().toString(), Map.of(), null));
    }

    /** Tenant-scoped fetch — cross-tenant ids surface as NOT_FOUND (no leakage). */
    private Lead requireOwned(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return leads.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "LEAD_NOT_FOUND", HttpStatus.NOT_FOUND, "Lead not found"));
    }
}
