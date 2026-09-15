package in.fixna.platform.security;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.fixna.platform.billing.PlanLimitChecker;
import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignChannelRepository;
import in.fixna.platform.campaign.CampaignOfferRepository;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.campaign.CampaignService;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.geo.GeoTarget;
import in.fixna.platform.geo.GeoTargetRepository;
import in.fixna.platform.geo.GeoTargetService;
import in.fixna.platform.geo.GeoTargetType;
import in.fixna.platform.lead.LeadRepository;
import in.fixna.platform.lead.LeadService;
import in.fixna.platform.lead.LeadStatus;
import in.fixna.platform.lead.dto.LeadStatusRequest;
import in.fixna.platform.notification.NotificationService;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * IDOR sweep: user A of tenant B can neither resolve nor pivot through tenant
 * A's rows. Every resource lookup below must surface as 404 (NOT_FOUND) — the
 * same code as a genuinely missing id — so no existence/or ownership
 * information leaks across tenants.
 */
@ExtendWith(MockitoExtension.class)
class CrossTenantSweepTest {

    private final UUID userA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID resourceOfTenantA = UUID.randomUUID();

    @Mock GeoTargetRepository geoTargets;
    @Mock CampaignRepository campaigns;
    @Mock CampaignChannelRepository campaignChannels;
    @Mock CampaignOfferRepository campaignOffers;
    @Mock BusinessRepository businesses;
    @Mock PlanLimitChecker planLimits;
    @Mock AuditPublisher audit;
    @Mock NotificationService notifications;

    @InjectMocks GeoTargetService geoService;

    @Mock LeadRepository leads;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private void tenantBContext() {
        TenantContext.set(tenantB, userA, MembershipRole.TENANT_MARKETING_USER);
    }

    @Test
    void crossTenantTargetLookupIsNotFound() {
        tenantBContext();
        when(campaigns.findByIdAndTenantId(resourceOfTenantA, tenantB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> geoService.remove(resourceOfTenantA, resourceOfTenantA))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
    }

    @Test
    void crossTenantLeadStatusChangeIsNotFound() {
        tenantBContext();
        when(leads.findByIdAndTenantId(resourceOfTenantA, tenantB)).thenReturn(Optional.empty());
        LeadService leadService =
                new LeadService(leads, businesses, campaigns, audit, notifications);

        assertThatThrownBy(() -> leadService.updateStatus(
                        resourceOfTenantA, new LeadStatusRequest(LeadStatus.CONTACTED)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("LEAD_NOT_FOUND");
    }

    @Test
    void crossTenantCampaignReadIsNotFound() {
        tenantBContext();
        CampaignService campaignService = new CampaignService(
                campaigns, campaignOffers, campaignChannels, businesses, audit, planLimits);
        when(campaigns.findByIdAndTenantId(resourceOfTenantA, tenantB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.get(resourceOfTenantA))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
    }

    @Test
    void campaignFromOtherTenantCannotParentGeoTargets() {
        tenantBContext();
        Campaign foreign = new Campaign();
        foreign.setId(resourceOfTenantA);
        foreign.setTenantId(tenantB);
        when(campaigns.findByIdAndTenantId(resourceOfTenantA, tenantB))
                .thenReturn(Optional.of(foreign));
        GeoTarget rogue = new GeoTarget();
        rogue.setId(UUID.randomUUID());
        rogue.setTenantId(tenantB);
        rogue.setCampaignId(UUID.randomUUID()); // belongs to a different campaign of tenant B
        rogue.setTargetType(GeoTargetType.CITY);
        rogue.setCity("Noida");
        when(geoTargets.findByIdAndTenantId(rogue.getId(), tenantB))
                .thenReturn(Optional.of(rogue));

        assertThatThrownBy(() -> geoService.remove(resourceOfTenantA, rogue.getId()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("GEO_TARGET_MISMATCH");
    }
}