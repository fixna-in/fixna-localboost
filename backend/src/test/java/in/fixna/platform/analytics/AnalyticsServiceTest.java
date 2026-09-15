package in.fixna.platform.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.analytics.dto.CampaignMetricsResponse;
import in.fixna.platform.analytics.dto.DashboardResponse;
import in.fixna.platform.analytics.dto.MetricUpsertRequest;
import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.lead.LeadStatus;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tenant isolation, RBAC, upsert idempotency and seed determinism (WF08). */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 11);
    private static final LocalDate FROM = DAY.minusDays(13);

    @Mock CampaignMetricRepository metrics;
    @Mock CampaignRepository campaigns;
    @Mock in.fixna.platform.lead.LeadRepository leads;
    @Mock in.fixna.platform.common.audit.AuditPublisher audit;

    @InjectMocks AnalyticsService service;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void dashboardTotalsAndFunnelAreTenantScoped() {
        CampaignMetric m1 = metric(1000, "100.00");
        CampaignMetric m2 = metric(500, "50.50");
        when(metrics.findByTenantIdAndMetricDateBetweenOrderByMetricDateAsc(eq(tenantA), eq(FROM), eq(DAY)))
                .thenReturn(List.of(m1, m2));
        when(leads.countByTenantIdAndStatus(eq(tenantA), eq(LeadStatus.NEW))).thenReturn(5L);
        when(leads.countByTenantIdAndStatus(eq(tenantA), eq(LeadStatus.CONTACTED))).thenReturn(2L);

        DashboardResponse dashboard = service.dashboard(FROM, DAY);

        assertThat(dashboard.totals().impressions()).isEqualTo(1500);
        assertThat(dashboard.totals().spend().compareTo(new BigDecimal("150.50"))).isZero();
        assertThat(dashboard.leads().newCount()).isEqualTo(5L);
        assertThat(dashboard.leads().contacted()).isEqualTo(2L);
    }

    @Test
    void dashboardFailsClosedWithoutTenantContext() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.dashboard(FROM, DAY)).isInstanceOf(FixnaException.class);
    }

    @Test
    void viewerCannotIngestMetrics() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_VIEWER);
        assertThatThrownBy(() -> service.ingest(campaignId, request()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(metrics, never()).save(any());
    }

    private CampaignMetric metric(long impressions, String spend) {
        CampaignMetric m = new CampaignMetric();
        m.setTenantId(tenantA);
        m.setCampaignId(campaignId);
        m.setMetricDate(DAY);
        m.setSpend(new BigDecimal(spend));
        m.setImpressions(impressions);
        m.setReach(impressions * 9 / 10);
        m.setClicks(impressions / 100);
        m.setConversions(impressions / 1000);
        m.setLeads(impressions / 2000);
        return m;
    }

    private MetricUpsertRequest request() {
        return new MetricUpsertRequest(DAY, new BigDecimal("25.00"), 100, 90, 10, 2, 4);
    }

    private MetricUpsertRequest request(BigDecimal spend, long impressions) {
        return new MetricUpsertRequest(DAY, spend, impressions, impressions * 9 / 10,
                impressions / 100, impressions / 1000, impressions / 2000);
    }

    @Test
    void ingestOverwritesExistingDayInsteadOfDuplicating() {
        CampaignMetric existing = metric(5, "10.00");
        when(campaigns.findByIdAndTenantId(campaignId, tenantA)).thenReturn(Optional.of(new Campaign()));
        when(metrics.findByTenantIdAndCampaignIdAndMetricDate(tenantA, campaignId, DAY))
                .thenReturn(Optional.of(existing));

        service.ingest(campaignId, request());

        ArgumentCaptor<CampaignMetric> captor = ArgumentCaptor.forClass(CampaignMetric.class);
        verify(metrics).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getSpend().compareTo(new BigDecimal("25.00"))).isZero();
        assertThat(captor.getValue().getImpressions()).isEqualTo(100);
        verify(audit).publish(any());
    }

    @Test
    void ingestCreatesNewMetricWhenDayAbsent() {
        when(campaigns.findByIdAndTenantId(campaignId, tenantA)).thenReturn(Optional.of(new Campaign()));
        when(metrics.findByTenantIdAndCampaignIdAndMetricDate(tenantA, campaignId, DAY))
                .thenReturn(Optional.empty());

        service.ingest(campaignId, request());

        ArgumentCaptor<CampaignMetric> captor = ArgumentCaptor.forClass(CampaignMetric.class);
        verify(metrics).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantA);
        assertThat(captor.getValue().getCampaignId()).isEqualTo(campaignId);
        assertThat(captor.getValue().getMetricDate()).isEqualTo(DAY);
        assertThat(captor.getValue().getImpressions()).isEqualTo(100);
    }

    @Test
    void campaignMetricsRejectsCrossTenantCampaign() {
        when(campaigns.findByIdAndTenantId(campaignId, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.campaignMetrics(campaignId, FROM, DAY))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
        verify(metrics, never()).save(any());
    }

    @Test
    void seedDemoRequiresCampaignId() {
        assertThatThrownBy(() -> service.seedDemo(null))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("VALIDATION_FAILED");
        verify(metrics, never()).save(any());
    }

    @Test
    void seedDemoIsDeterministicAndSeedsTwoWeeks() {
        when(campaigns.findByIdAndTenantId(campaignId, tenantA)).thenReturn(Optional.of(new Campaign()));
        stubAbsent();
        var first = service.seedDemo(campaignId);
        List<Long> firstRun = captureImpressions();

        clearInvocations(metrics, audit);
        stubAbsent();
        var second = service.seedDemo(campaignId);
        List<Long> secondRun = captureImpressions();

        assertThat(first.campaigns()).isEqualTo(1);
        assertThat(first.metricDays()).isEqualTo(14);
        assertThat(second).isEqualTo(first);
        assertThat(secondRun).isEqualTo(firstRun);
        verify(audit).publish(any());
    }

    private void stubAbsent() {
        when(metrics.findByTenantIdAndCampaignIdAndMetricDate(eq(tenantA), eq(campaignId), any()))
                .thenReturn(Optional.empty());
    }

    private List<Long> captureImpressions() {
        ArgumentCaptor<CampaignMetric> captor = ArgumentCaptor.forClass(CampaignMetric.class);
        verify(metrics, times(14)).save(captor.capture());
        return captor.getAllValues().stream().map(CampaignMetric::getImpressions).toList();
    }
}
