package in.fixna.platform.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.analytics.dto.CampaignMetricsResponse;
import in.fixna.platform.analytics.dto.DashboardResponse;
import in.fixna.platform.analytics.dto.MetricTotals;
import in.fixna.platform.analytics.dto.MetricUpsertRequest;
import in.fixna.platform.analytics.dto.SeedResult;
import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.lead.LeadRepository;
import in.fixna.platform.lead.LeadStatus;

/**
 * Analytics application service (dashboard / campaign timeline / metric
 * ingest / deterministic demo seed). Every query is tenant-scoped from
 * {@link TenantContext}; ingest is an idempotent upsert on the DB's
 * UNIQUE(campaign_id, metric_date) key; the demo seed produces the same
 * values per (campaign, day) so re-seeding overwrites instead of drifting.
 */
@Service
public class AnalyticsService {

    static final int DEFAULT_WINDOW_DAYS = 30;
    static final int SEED_DAYS = 14;

    private final CampaignMetricRepository metrics;
    private final CampaignRepository campaigns;
    private final LeadRepository leads;
    private final AuditPublisher audit;

    public AnalyticsService(
            CampaignMetricRepository metrics,
            CampaignRepository campaigns,
            LeadRepository leads,
            AuditPublisher audit) {
        this.metrics = metrics;
        this.campaigns = campaigns;
        this.leads = leads;
        this.audit = audit;
    }

    /** Tenant-wide dashboard: window totals, per-campaign rollup, lead funnel. */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(LocalDate from, LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate windowTo = to != null ? to : LocalDate.now();
        LocalDate windowFrom = from != null ? from : windowTo.minusDays(DEFAULT_WINDOW_DAYS - 1L);
        List<CampaignMetric> window = metrics
                .findByTenantIdAndMetricDateBetweenOrderByMetricDateAsc(tenantId, windowFrom, windowTo);

        MetricTotals totals = MetricTotals.zero();
        Map<UUID, List<CampaignMetric>> byCampaign = new LinkedHashMap<>();
        for (CampaignMetric metric : window) {
            totals = add(totals, metric);
            byCampaign.computeIfAbsent(metric.getCampaignId(), id -> new ArrayList<>()).add(metric);
        }
        List<DashboardResponse.CampaignRollup> rollups = byCampaign.entrySet().stream()
                .map(entry -> new DashboardResponse.CampaignRollup(
                        entry.getKey(), campaignName(tenantId, entry.getKey()), sum(entry.getValue())))
                .toList();
        DashboardResponse.LeadFunnel funnel = new DashboardResponse.LeadFunnel(
                leads.countByTenantIdAndStatus(tenantId, LeadStatus.NEW),
                leads.countByTenantIdAndStatus(tenantId, LeadStatus.CONTACTED),
                leads.countByTenantIdAndStatus(tenantId, LeadStatus.QUALIFIED),
                leads.countByTenantIdAndStatus(tenantId, LeadStatus.CONVERTED),
                leads.countByTenantIdAndStatus(tenantId, LeadStatus.LOST));
        return new DashboardResponse(windowFrom, windowTo, totals, rollups, funnel);
    }

    /** Campaign-scoped timeline + totals (tenant-scoped, window defaults). */
    @Transactional(readOnly = true)
    public CampaignMetricsResponse campaignMetrics(UUID campaignId, LocalDate from, LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        Campaign campaign = requireOwnedCampaign(tenantId, campaignId);
        LocalDate windowTo = to != null ? to : LocalDate.now();
        LocalDate windowFrom = from != null ? from : windowTo.minusDays(DEFAULT_WINDOW_DAYS - 1L);
        List<CampaignMetric> timeline = metrics
                .findByTenantIdAndCampaignIdAndMetricDateBetweenOrderByMetricDateAsc(
                        tenantId, campaignId, windowFrom, windowTo);
        return CampaignMetricsResponse.of(
                campaignId, campaign.getName(), windowFrom, windowTo, sum(timeline), timeline);
    }

    /** Idempotent metric ingest: upsert on UNIQUE(campaign_id, metric_date). */
    @Transactional
    public void ingest(UUID campaignId, MetricUpsertRequest request) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        requireOwnedCampaign(tenantId, campaignId);
        CampaignMetric metric = metrics
                .findByTenantIdAndCampaignIdAndMetricDate(tenantId, campaignId, request.metricDate())
                .orElseGet(CampaignMetric::new);
        metric.setTenantId(tenantId);
        metric.setCampaignId(campaignId);
        metric.setMetricDate(request.metricDate());
        metric.setSpend(request.spend());
        metric.setImpressions(request.impressions());
        metric.setReach(request.reach());
        metric.setClicks(request.clicks());
        metric.setConversions(request.conversions());
        metric.setLeads(request.leads());
        metrics.save(metric);
        audit.publish(new AuditEvent(
                "analytics.ingested", tenantId, TenantContext.requireUserId(), "campaign_metric",
                campaignId.toString(), Map.of("date", request.metricDate().toString()), null));
    }

    /** Deterministic demo seed for one campaign: same (campaign, day) → same values. */
    @Transactional
    public SeedResult seedDemo(UUID campaignId) {
        TenantContext.requireWrite();
        UUID tenantId = TenantContext.requireTenantId();
        if (campaignId == null) {
            throw new FixnaException(
                    "VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
                    "campaignId is required for demo seeding");
        }
        requireOwnedCampaign(tenantId, campaignId);
        LocalDate today = LocalDate.now();
        for (int offset = SEED_DAYS - 1; offset >= 0; offset--) {
            upsertDeterministic(tenantId, campaignId, today.minusDays(offset));
        }
        audit.publish(new AuditEvent(
                "analytics.demo_seeded", tenantId, TenantContext.requireUserId(), "campaign",
                campaignId.toString(), Map.of("days", String.valueOf(SEED_DAYS)), null));
        return new SeedResult(1, SEED_DAYS);
    }

    private void upsertDeterministic(UUID tenantId, UUID campaignId, LocalDate day) {
        // Seed per (campaign, day): re-seeding produces byte-identical values
        // and the upsert overwrites, so demo data never drifts or duplicates.
        Random random = new Random(campaignId.getMostSignificantBits() ^ day.toEpochDay());
        CampaignMetric metric = metrics
                .findByTenantIdAndCampaignIdAndMetricDate(tenantId, campaignId, day)
                .orElseGet(CampaignMetric::new);
        metric.setTenantId(tenantId);
        metric.setCampaignId(campaignId);
        metric.setMetricDate(day);
        double impressions = 4000 + random.nextDouble() * 12000;
        double reach = impressions * (0.5 + random.nextDouble() * 0.3);
        double clicks = impressions * (0.01 + random.nextDouble() * 0.02);
        double conversions = clicks * (0.05 + random.nextDouble() * 0.1);
        metric.setSpend(BigDecimal.valueOf(Math.round((1500 + random.nextDouble() * 3000) * 100.0), 2));
        metric.setImpressions(Math.round(impressions));
        metric.setReach(Math.round(reach));
        metric.setClicks(Math.round(clicks));
        metric.setConversions(Math.round(conversions));
        metric.setLeads(Math.round(conversions) + random.nextInt(3));
        metrics.save(metric);
    }

    private Campaign requireOwnedCampaign(UUID tenantId, UUID campaignId) {
        return campaigns
                .findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new FixnaException(
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private MetricTotals add(MetricTotals totals, CampaignMetric metric) {
        return new MetricTotals(
                totals.spend().add(metric.getSpend() == null ? BigDecimal.ZERO : metric.getSpend()),
                totals.impressions() + metric.getImpressions(),
                totals.reach() + metric.getReach(),
                totals.clicks() + metric.getClicks(),
                totals.conversions() + metric.getConversions(),
                totals.leads() + metric.getLeads());
    }

    private MetricTotals sum(List<CampaignMetric> list) {
        MetricTotals totals = MetricTotals.zero();
        for (CampaignMetric metric : list) {
            totals = add(totals, metric);
        }
        return totals;
    }

    private String campaignName(UUID tenantId, UUID campaignId) {
        return campaigns.findByIdAndTenantId(campaignId, tenantId)
                .map(Campaign::getName)
                .orElse("Unknown campaign");
    }
}
