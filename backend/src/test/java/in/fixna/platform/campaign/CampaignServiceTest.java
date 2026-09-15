package in.fixna.platform.campaign;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.business.Business;
import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.dto.CampaignOfferRequest;
import in.fixna.platform.campaign.dto.CampaignRequest;
import in.fixna.platform.campaign.dto.CampaignResponse;
import in.fixna.platform.campaign.dto.ChannelAllocation;
import in.fixna.platform.campaign.dto.ChannelAllocationRequest;
import in.fixna.platform.campaign.dto.TransitionRequest;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Workflow 04 unit tests: campaign CRUD + lifecycle (BR-1/3/4/5/6) and
 * tenant-isolation (cross-tenant ids surface as NOT_FOUND; viewer is
 * read-only; launch is idempotent).
 */
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock CampaignRepository campaigns;
    @Mock CampaignOfferRepository offers;
    @Mock CampaignChannelRepository channels;
    @Mock BusinessRepository businesses;
    @Mock AuditPublisher audit;
    @Mock in.fixna.platform.billing.PlanLimitChecker planLimits;

    @InjectMocks CampaignService service;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID businessA = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private void ctx(MembershipRole role) {
        TenantContext.set(tenantA, userA, role);
    }

    private Campaign campaign(CampaignStatus status) {
        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setTenantId(tenantA);
        campaign.setBusinessId(businessA);
        campaign.setName("Monsoon Offer");
        campaign.setObjective(CampaignObjective.PROMOTION);
        campaign.setTotalBudget(new BigDecimal("1000.00"));
        campaign.setCurrency("INR");
        campaign.setStatus(status);
        campaign.setExternalReference(UUID.randomUUID().toString());
        return campaign;
    }

    private CampaignRequest request() {
        return new CampaignRequest(
                businessA,
                "Monsoon Offer",
                CampaignObjective.PROMOTION,
                new BigDecimal("1000.00"),
                "INR",
                null,
                null);
    }

    @Test
    void createSavesDraftWithGeneratedExternalReference() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        Business business = new Business();
        business.setId(businessA);
        business.setTenantId(tenantA);
        when(businesses.findByIdAndTenantId(businessA, tenantA)).thenReturn(Optional.of(business));
        // Emulate JPA @PrePersist (does not run under Mockito).
        when(campaigns.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            if (c.getExternalReference() == null) {
                c.setExternalReference(UUID.randomUUID().toString());
            }
            if (c.getStatus() == null) {
                c.setStatus(CampaignStatus.DRAFT);
            }
            return c;
        });

        CampaignResponse response = service.create(request());

        assertThat(response.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(response.externalReference()).isNotBlank();
        verify(campaigns).save(any(Campaign.class));
    }

    @Test
    void createRejectsBusinessOutsideTenantAsNotFound() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(businesses.findByIdAndTenantId(businessA, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("BUSINESS_NOT_FOUND");
    }

    @Test
    void viewerCannotCreateCampaign() {
        ctx(MembershipRole.TENANT_VIEWER);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateOnlyAllowedInDraft() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        Campaign campaign = campaign(CampaignStatus.READY_FOR_REVIEW);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.update(campaign.getId(), request()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_LOCKED");
    }

    @Test
    void deleteBlockedForApprovedCampaign() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        Campaign campaign = campaign(CampaignStatus.APPROVED);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.delete(campaign.getId()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_LOCKED");
    }

    @Test
    void approvalPathTravelsDraftToApproved() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        CampaignResponse review =
                service.transition(campaign.getId(), new TransitionRequest(CampaignStatus.READY_FOR_REVIEW));
        assertThat(review.status()).isEqualTo(CampaignStatus.READY_FOR_REVIEW);

        CampaignResponse approved =
                service.transition(campaign.getId(), new TransitionRequest(CampaignStatus.APPROVED));
        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
    }

    @Test
    void illegalTransitionRejected() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.transition(
                        campaign.getId(), new TransitionRequest(CampaignStatus.ACTIVE)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("ILLEGAL_TRANSITION");
    }

    @Test
    void queuedCannotBeReachedByTransitionEndpoint() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.APPROVED);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.transition(
                        campaign.getId(), new TransitionRequest(CampaignStatus.QUEUED)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("ILLEGAL_TRANSITION");
    }

    @Test
    void launchRequiresApproval() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.launch(campaign.getId()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_APPROVED");
    }

    @Test
    void approvedLaunchMovesToQueued() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.APPROVED);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        CampaignResponse response = service.launch(campaign.getId());

        assertThat(response.status()).isEqualTo(CampaignStatus.QUEUED);
    }

    @Test
    void launchIsIdempotentWhenAlreadyQueued() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.QUEUED);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        CampaignResponse response = service.launch(campaign.getId());

        assertThat(response.status()).isEqualTo(CampaignStatus.QUEUED);
    }

    @Test
    void failedLaunchCanBeRetried() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.FAILED);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));

        CampaignResponse response = service.launch(campaign.getId());

        assertThat(response.status()).isEqualTo(CampaignStatus.QUEUED);
    }

    @Test
    void addOfferRequiresOwnedCampaign() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(any(), any())).thenReturn(Optional.of(campaign(CampaignStatus.DRAFT)));
        when(offers.save(any(CampaignOffer.class))).thenAnswer(inv -> {
            CampaignOffer o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        var response = service.addOffer(
                UUID.randomUUID(), new CampaignOfferRequest("Flat 20%", "On mains", "MONSOON20"));

        assertThat(response.title()).isEqualTo("Flat 20%");
        verify(offers).save(any(CampaignOffer.class));
    }

    @Test
    void replaceChannelsExceedingBudgetRejected() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));
        ChannelAllocationRequest allocation =
                new ChannelAllocationRequest(List.of(
                        new ChannelAllocation("google", new BigDecimal("700.00")),
                        new ChannelAllocation("meta", new BigDecimal("400.00"))));

        assertThatThrownBy(() -> service.replaceChannels(campaign.getId(), allocation))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("BUDGET_EXCEEDED");
    }

    @Test
    void duplicateChannelRejected() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));
        ChannelAllocationRequest allocation =
                new ChannelAllocationRequest(List.of(
                        new ChannelAllocation("google", new BigDecimal("300.00")),
                        new ChannelAllocation("GOOGLE", new BigDecimal("200.00"))));

        assertThatThrownBy(() -> service.replaceChannels(campaign.getId(), allocation))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("DUPLICATE_CHANNEL");
    }

    @Test
    void channelsLockedAfterApproval() {
        ctx(MembershipRole.TENANT_MARKETING_MANAGER);
        Campaign campaign = campaign(CampaignStatus.APPROVED);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));
        ChannelAllocationRequest allocation =
                new ChannelAllocationRequest(List.of(
                        new ChannelAllocation("google", new BigDecimal("500.00"))));

        assertThatThrownBy(() -> service.replaceChannels(campaign.getId(), allocation))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_LOCKED");
    }

    @Test
    void replaceChannelsSavesAllocationsWithinBudget() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaigns.findByIdAndTenantId(campaign.getId(), tenantA)).thenReturn(Optional.of(campaign));
        when(channels.findByTenantIdAndCampaignId(tenantA, campaign.getId())).thenReturn(List.of());
        when(channels.save(any(CampaignChannel.class))).thenAnswer(inv -> {
            CampaignChannel ch = inv.getArgument(0);
            ch.setId(UUID.randomUUID());
            return ch;
        });
        ChannelAllocationRequest allocation =
                new ChannelAllocationRequest(List.of(
                        new ChannelAllocation("google", new BigDecimal("600.00")),
                        new ChannelAllocation("meta", new BigDecimal("300.00"))));

        var response = service.replaceChannels(campaign.getId(), allocation);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).channel()).isEqualTo("GOOGLE");
        verify(channels, org.mockito.Mockito.times(2)).save(any(CampaignChannel.class));
    }
}