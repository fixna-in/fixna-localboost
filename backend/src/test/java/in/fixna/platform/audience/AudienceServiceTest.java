package in.fixna.platform.audience;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audience.dto.AudienceRequest;
import in.fixna.platform.audience.dto.AudienceResponse;
import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Workflow 05 audience tests: definition validation (unknown keys, bad
 * ranges, empty lists), tenant isolation, viewer read-only and
 * campaign-bound updates.
 */
@ExtendWith(MockitoExtension.class)
class AudienceServiceTest {

    @Mock AudienceRepository audiences;
    @Mock CampaignRepository campaigns;
    @Mock AuditPublisher audit;

    @InjectMocks AudienceService service;

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

    private AudienceRequest valid() {
        return new AudienceRequest("Noida Families", Map.of(
                "ageMin", 25, "ageMax", 45,
                "genders", List.of("female"),
                "incomeBrackets", List.of("30000-60000")));
    }
@Test
    void createWithValidDefinitionSaves() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        when(audiences.save(any(Audience.class))).thenAnswer(inv -> {
            Audience a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AudienceResponse response = service.create(campaignA, valid());

        assertThat(response.name()).isEqualTo("Noida Families");
        assertThat(response.definition()).containsEntry("ageMin", 25);
    }

    @Test
    void unknownCriterionKeyRejected() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        AudienceRequest request =
                new AudienceRequest("Bad", Map.of("ageRng", 30));

        assertThatThrownBy(() -> service.create(campaignA, request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_AUDIENCE");
    }

    @Test
    void invertedAgeRangeRejected() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        AudienceRequest request = new AudienceRequest("Bad", Map.of("ageMin", 50, "ageMax", 18));

        assertThatThrownBy(() -> service.create(campaignA, request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_AUDIENCE");
    }

    @Test
    void emptyInterestListRejected() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        AudienceRequest request = new AudienceRequest("Bad", Map.of("interests", List.of()));

        assertThatThrownBy(() -> service.create(campaignA, request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_AUDIENCE");
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
    void viewerCannotCreateAudience() {
        ctx(MembershipRole.TENANT_VIEWER);

        assertThatThrownBy(() -> service.create(campaignA, valid()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateRejectsAudienceOfAnotherCampaign() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        Audience otherCampaignAudience = new Audience();
        otherCampaignAudience.setId(UUID.randomUUID());
        otherCampaignAudience.setTenantId(tenantA);
        otherCampaignAudience.setCampaignId(UUID.randomUUID());
        when(audiences.findByIdAndTenantId(otherCampaignAudience.getId(), tenantA))
                .thenReturn(Optional.of(otherCampaignAudience));

        assertThatThrownBy(() -> service.update(campaignA, otherCampaignAudience.getId(), valid()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AUDIENCE_MISMATCH");
    }
}