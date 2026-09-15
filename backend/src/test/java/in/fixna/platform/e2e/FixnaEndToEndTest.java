package in.fixna.platform.e2e;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import in.fixna.platform.ai.AiRecommendationService;
import in.fixna.platform.ai.RecommendationResult;
import in.fixna.platform.ai.RecommendationType;
import in.fixna.platform.analytics.AnalyticsService;
import in.fixna.platform.analytics.dto.DashboardResponse;
import in.fixna.platform.analytics.dto.MetricUpsertRequest;
import in.fixna.platform.audience.AudienceService;
import in.fixna.platform.audience.dto.AudienceRequest;
import in.fixna.platform.auth.AuthService;
import in.fixna.platform.auth.dto.AuthResponse;
import in.fixna.platform.auth.dto.RegisterRequest;
import in.fixna.platform.business.BusinessService;
import in.fixna.platform.business.dto.BusinessLocationRequest;
import in.fixna.platform.business.dto.BusinessLocationResponse;
import in.fixna.platform.business.dto.BusinessRequest;
import in.fixna.platform.business.dto.BusinessResponse;
import in.fixna.platform.campaign.CampaignLaunchService;
import in.fixna.platform.campaign.CampaignObjective;
import in.fixna.platform.campaign.CampaignService;
import in.fixna.platform.campaign.CampaignStatus;
import in.fixna.platform.campaign.dto.CampaignRequest;
import in.fixna.platform.campaign.dto.CampaignResponse;
import in.fixna.platform.campaign.dto.ChannelAllocation;
import in.fixna.platform.campaign.dto.ChannelAllocationRequest;
import in.fixna.platform.campaign.dto.TransitionRequest;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.geo.GeoTargetService;
import in.fixna.platform.geo.GeoTargetType;
import in.fixna.platform.geo.dto.GeoTargetRequest;
import in.fixna.platform.lead.LeadService;
import in.fixna.platform.lead.dto.LeadRequest;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Workflow 12 — final end-to-end journey against a real PostgreSQL
 * (Testcontainers) with all Flyway migrations applied:
 * register → tenant → business → location → campaign → geo → audience →
 * budget/channels → AI recommendation (mock, no credentials) → review →
 * approve → mock launch → metrics → lead creation → analytics. Also verifies
 * cross-tenant denial, invalid input rejection and idempotent duplicate
 * launch. Skipped automatically when Docker is unavailable; runs in CI.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfDockerAvailable
class FixnaEndToEndTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("fixna")
                    .withUsername("fixna")
                    .withPassword("fixna");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired AuthService auth;
    @Autowired BusinessService businessService;
    @Autowired CampaignService campaigns;
    @Autowired GeoTargetService geo;
    @Autowired AudienceService audiences;
    @Autowired AiRecommendationService ai;
    @Autowired CampaignLaunchService launcher;
    @Autowired AnalyticsService analytics;
    @Autowired LeadService leads;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private AuthResponse register(String email, String tenantName) {
        return auth.register(new RegisterRequest(email, "Password123!", "Owner", "One", tenantName));
    }

    private BusinessResponse createBusiness(UUID user) {
        return businessService.create(new BusinessRequest(
                "Cafe Diwali", "restaurants", "Family cafe in Sector 62", "https://cafe.example", "+91 98100 00000"));
    }

    private CampaignResponse createCampaign(UUID businessId) {
        return campaigns.create(new CampaignRequest(
                businessId, "Diwali Promo", CampaignObjective.PROMOTION,
                new BigDecimal("1000.00"), "INR", null, null));
    }

    private GeoTargetRequest noidaCityTarget() {
        return new GeoTargetRequest(GeoTargetType.CITY, "Noida", null, null, null, null, null, "Noida", null);
    }

    @Test
    void fullLocalBoostJourneyCoversRegisterToAnalyticsInDemoMode() {
        AuthResponse owner = register("owner-" + UUID.randomUUID() + "@example.com", "Diwali Cafe");
        TenantContext.set(owner.tenantId(), owner.userId(), MembershipRole.TENANT_OWNER);

        BusinessResponse business = createBusiness(owner.userId());
        assertThat(business.id()).isNotNull();

        BusinessLocationResponse location = businessService.addLocation(
                business.id(), new BusinessLocationRequest(
                        "Sector 62, Noida", "Noida", "UP", "201301", "IN", null, null));
        assertThat(location.id()).isNotNull();

        CampaignResponse campaign = createCampaign(business.id());
        assertThat(campaign.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(campaign.totalBudget()).isEqualByComparingTo("1000.00");

        geo.add(campaign.id(), noidaCityTarget());
        audiences.create(campaign.id(), new AudienceRequest(
                "Noida families", Map.of("ageMin", 25, "ageMax", 45, "genders", List.of("ALL"))));

        // Budget (BR-3/BR-4): total 1000, allocations 400 + 600.
        var channels = campaigns.replaceChannels(campaign.id(),
                new ChannelAllocationRequest(List.of(
                        new ChannelAllocation("GOOGLE", new BigDecimal("400.00")),
                        new ChannelAllocation("META", new BigDecimal("600.00")))));
        assertThat(channels).hasSize(2);

        // AI recommendation in demo mode — mock provider, zero credentials.
        RecommendationResult rec = ai.recommend(RecommendationType.CAMPAIGN_STRATEGY,
                business.id(), campaign.id(), Map.of("budget", 1000));
        assertThat(rec.provider()).isEqualTo("mock");
        assertThat(rec.data()).isNotEmpty();

        // Review → approve → launch → execute through mock adapters.
        campaigns.transition(campaign.id(), new TransitionRequest(CampaignStatus.READY_FOR_REVIEW));
        campaigns.transition(campaign.id(), new TransitionRequest(CampaignStatus.APPROVED));
        assertThat(campaigns.launch(campaign.id()).status()).isEqualTo(CampaignStatus.QUEUED);
        assertThat(launcher.run(campaign.id()).status()).isEqualTo(CampaignStatus.ACTIVE);

        // BR-6 idempotency: repeated launch and re-execution are safe no-ops.
        assertThat(campaigns.launch(campaign.id()).status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(launcher.run(campaign.id()).status()).isEqualTo(CampaignStatus.ACTIVE);

        // Metrics ingest → lead creation → analytics dashboard.
        analytics.ingest(campaign.id(), new MetricUpsertRequest(
                LocalDate.now(), new BigDecimal("250.00"), 1200, 900, 24, 3, 2));
        var lead = leads.create(business.id(), new LeadRequest(
                "Ravi", "+91 98100 12345", "ravi@example.com", campaign.id(), "google"));
        assertThat(lead.id()).isNotNull();

        DashboardResponse dashboard =
                analytics.dashboard(LocalDate.now().minusDays(7), LocalDate.now());
        assertThat(dashboard.totals().leads()).isGreaterThanOrEqualTo(1L);
        assertThat(dashboard.campaigns()).anyMatch(rollup -> rollup.campaignId().equals(campaign.id()));
    }

    @Test
    void crossTenantCannotAccessAnotherTenantsResources() {
        AuthResponse a = register("a-" + UUID.randomUUID() + "@example.com", "Tenant A");
        TenantContext.set(a.tenantId(), a.userId(), MembershipRole.TENANT_OWNER);
        BusinessResponse bizA = createBusiness(a.userId());
        CampaignResponse camA = createCampaign(bizA.id());
        TenantContext.clear();

        AuthResponse b = register("b-" + UUID.randomUUID() + "@example.com", "Tenant B");
        TenantContext.set(b.tenantId(), b.userId(), MembershipRole.TENANT_OWNER);

        assertThatThrownBy(() -> campaigns.get(camA.id()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
        assertThatThrownBy(() -> businessService.get(bizA.id()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("BUSINESS_NOT_FOUND");
    }

    @Test
    void invalidInputIsRejected() {
        AuthResponse owner = register("invalid-" + UUID.randomUUID() + "@example.com", "Invalid Cafe");
        TenantContext.set(owner.tenantId(), owner.userId(), MembershipRole.TENANT_OWNER);
        BusinessResponse biz = createBusiness(owner.userId());
        CampaignResponse camp = createCampaign(biz.id());

        // BR-4: allocations exceeding the total budget are rejected.
        assertThatThrownBy(() -> campaigns.replaceChannels(camp.id(),
                new ChannelAllocationRequest(List.of(
                        new ChannelAllocation("GOOGLE", new BigDecimal("700.00")),
                        new ChannelAllocation("META", new BigDecimal("400.00"))))))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("BUDGET_EXCEEDED");

        // Geo: CITY target without a city is rejected.
        assertThatThrownBy(() -> geo.add(camp.id(),
                new GeoTargetRequest(GeoTargetType.CITY, null, null, null, null, null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_GEO_TARGET");

        // AI: malformed budget-allocation request is rejected before a provider call.
        assertThatThrownBy(() -> ai.recommend(RecommendationType.BUDGET_ALLOCATION,
                biz.id(), camp.id(), Map.of("budget", 100, "channels", List.of())))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("AI_VALIDATION_FAILED");
    }
}