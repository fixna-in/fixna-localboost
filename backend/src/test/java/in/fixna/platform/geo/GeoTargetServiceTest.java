package in.fixna.platform.geo;

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

import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.geo.dto.GeoTargetRequest;
import in.fixna.platform.geo.dto.GeoTargetResponse;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Workflow 05 geo tests: per-type dimension validation, tenant isolation
 * (cross-tenant campaign -> NOT_FOUND), viewer read-only, campaign-bound
 * update/delete.
 */
@ExtendWith(MockitoExtension.class)
class GeoTargetServiceTest {

    @Mock GeoTargetRepository targets;
    @Mock CampaignRepository campaigns;
    @Mock AuditPublisher audit;

    @InjectMocks GeoTargetService service;

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

    @Test
    void radiusTargetRequiresAllThreeDimensions() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        GeoTargetRequest missingRadius =
                new GeoTargetRequest(GeoTargetType.RADIUS, null, new BigDecimal("28.6139"),
                        new BigDecimal("77.2090"), null, null, null, null, null);
        GeoTargetRequest negativeRadius =
                new GeoTargetRequest(GeoTargetType.RADIUS, null, new BigDecimal("28.6139"),
                        new BigDecimal("77.2090"), BigDecimal.ZERO, null, null, null, null);

        assertThatThrownBy(() -> service.add(campaignA, missingRadius))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_GEO_TARGET");
        assertThatThrownBy(() -> service.add(campaignA, negativeRadius))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_GEO_TARGET");
    }

    @Test
    void cityTargetRejectsPostalOnlyRequest() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        GeoTargetRequest request =
                new GeoTargetRequest(GeoTargetType.CITY, null, null, null, null, null, null, null, "201301");

        assertThatThrownBy(() -> service.add(campaignA, request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_GEO_TARGET");
    }

    @Test
    void validRadiusSavesWithCampaignAndTenant() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        when(targets.save(any(GeoTarget.class))).thenAnswer(inv -> {
            GeoTarget g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });
        GeoTargetRequest request =
                new GeoTargetRequest(GeoTargetType.RADIUS, "GT Noida 5km",
                        new BigDecimal("28.6139"), new BigDecimal("77.2090"),
                        new BigDecimal("5.00"), null, null, null, null);

        GeoTargetResponse response = service.add(campaignA, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.targetType()).isEqualTo(GeoTargetType.RADIUS);
        assertThat(response.radiusKm()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void crossTenantCampaignSurfacesNotFound() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        UUID campaignOfB = UUID.randomUUID();
        when(campaigns.findByIdAndTenantId(campaignOfB, tenantA)).thenReturn(Optional.empty());
        GeoTargetRequest request =
                new GeoTargetRequest(GeoTargetType.POSTAL, null, null, null, null,
                        null, null, null, "110001");

        assertThatThrownBy(() -> service.add(campaignOfB, request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
    }

    @Test
    void viewerCannotAddGeoTarget() {
        ctx(MembershipRole.TENANT_VIEWER);
        GeoTargetRequest request =
                new GeoTargetRequest(GeoTargetType.POSTAL, null, null, null, null,
                        null, null, null, "110001");

        assertThatThrownBy(() -> service.add(campaignA, request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listIsScopedToCampaign() {
        ctx(MembershipRole.TENANT_VIEWER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        GeoTarget target = new GeoTarget();
        target.setId(UUID.randomUUID());
        target.setTenantId(tenantA);
        target.setCampaignId(campaignA);
        target.setTargetType(GeoTargetType.COUNTRY);
        target.setCountryCode("IN");
        when(targets.findByTenantIdAndCampaignId(tenantA, campaignA)).thenReturn(List.of(target));

        List<GeoTargetResponse> result = service.list(campaignA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).countryCode()).isEqualTo("IN");
    }

    @Test
    void updateRejectsTargetOfAnotherCampaign() {
        ctx(MembershipRole.TENANT_MARKETING_USER);
        when(campaigns.findByIdAndTenantId(campaignA, tenantA)).thenReturn(Optional.of(campaign()));
        GeoTarget otherCampaignTarget = new GeoTarget();
        otherCampaignTarget.setId(UUID.randomUUID());
        otherCampaignTarget.setTenantId(tenantA);
        otherCampaignTarget.setCampaignId(UUID.randomUUID());
        otherCampaignTarget.setTargetType(GeoTargetType.POSTAL);
        otherCampaignTarget.setPostalCode("110001");
        when(targets.findByIdAndTenantId(otherCampaignTarget.getId(), tenantA))
                .thenReturn(Optional.of(otherCampaignTarget));
        GeoTargetRequest request =
                new GeoTargetRequest(GeoTargetType.POSTAL, null, null, null, null,
                        null, null, null, "201301");

        assertThatThrownBy(() -> service.update(campaignA, otherCampaignTarget.getId(), request))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("GEO_TARGET_MISMATCH");
    }
}