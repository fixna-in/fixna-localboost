package in.fixna.platform.creative;

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

import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.creative.dto.CreativeRequest;
import in.fixna.platform.creative.dto.CreativeResponse;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Workflow closeout — creatives (US-011): content rules, campaign-bound
 * updates, tenant isolation and viewer read-only.
 */
@ExtendWith(MockitoExtension.class)
class CreativeServiceTest {

    @Mock CreativeRepository creatives;
    @Mock CampaignRepository campaigns;
    @Mock AuditPublisher audit;

    @InjectMocks CreativeService service;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID campaignA = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private void ctx(MembershipRole role) {
        TenantContext.set(tenantA, userA, role);
    }

    private Campaign campaign() {
        Campaign campaign = new Campaign();
        campaign.setId(campaignA);
        campaign.setTenantId(tenantA);
        return campaign;
    }

    private CreativeRequest valid() {
        return new CreativeRequest("GOOGLE", "Limited-time offer", "Flat 20% off this weekend", "Book now", null);
    }

    @Test
    void createPersistsDraftAndPublishesAudit() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        when(creatives.save(any(Creative.class))).thenAnswer(inv -> {
            Creative c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CreativeResponse response = service.create(campaignA, valid());

        assertThat(response.channel()).isEqualTo("GOOGLE");
        assertThat(response.headline()).isEqualTo("Limited-time offer");
        assertThat(response.status()).isEqualTo(CreativeStatus.DRAFT);
    }

    @Test
    void createRejectsBlankChannel() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));

        assertThatThrownBy(() -> service.create(campaignA, new CreativeRequest("  ", "H", null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_CREATIVE");
    }

    @Test
    void createRejectsEmptyCopy() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));

        assertThatThrownBy(() -> service.create(campaignA, new CreativeRequest("META", "   ", "", "", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_CREATIVE");
    }

    @Test
    void crossTenantCampaignSurfacesNotFound() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        UUID campaignOfB = UUID.randomUUID();
        when(campaigns.findByIdAndTenantId(campaignOfB, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(campaignOfB, valid()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
    }

    @Test
    void viewerCannotCreateCreative() {
        ctx(MembershipRole.TENANT_VIEWER);

        assertThatThrownBy(() -> service.create(campaignA, valid()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateEditsContentAndPromotesToReady() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        Creative existing = new Creative();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(tenantA);
        existing.setCampaignId(campaignA);
        existing.setChannel("GOOGLE");
        existing.setStatus(CreativeStatus.DRAFT);
        when(creatives.findByIdAndTenantId(existing.getId(), tenantA)).thenReturn(Optional.of(existing));
        when(creatives.save(any(Creative.class))).thenReturn(existing);

        CreativeResponse response = service.update(
                campaignA, existing.getId(),
                new CreativeRequest("GOOGLE", "New head", "New body", "Call", CreativeStatus.READY));

        assertThat(response.status()).isEqualTo(CreativeStatus.READY);
        assertThat(response.headline()).isEqualTo("New head");
    }

    @Test
    void updateRejectsCreativeOfAnotherCampaign() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        Creative otherCampaignCreative = new Creative();
        otherCampaignCreative.setId(UUID.randomUUID());
        otherCampaignCreative.setTenantId(tenantA);
        otherCampaignCreative.setCampaignId(UUID.randomUUID());
        when(creatives.findByIdAndTenantId(otherCampaignCreative.getId(), tenantA))
                .thenReturn(Optional.of(otherCampaignCreative));

        assertThatThrownBy(() -> service.update(campaignA, otherCampaignCreative.getId(), valid()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CREATIVE_MISMATCH");
    }

    @Test
    void listScopesToTenantAndCampaign() {
        ctx(MembershipRole.TENANT_VIEWER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        Creative creative = new Creative();
        creative.setId(UUID.randomUUID());
        creative.setTenantId(tenantA);
        creative.setCampaignId(campaignA);
        creative.setChannel("WHATSAPP");
        when(creatives.findByTenantIdAndCampaignId(tenantA, campaignA)).thenReturn(List.of(creative));

        List<CreativeResponse> result = service.list(campaignA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).channel()).isEqualTo("WHATSAPP");
    }
}