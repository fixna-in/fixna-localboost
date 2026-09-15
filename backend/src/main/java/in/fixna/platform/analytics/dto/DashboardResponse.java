package in.fixna.platform.analytics.dto;

import java.util.List;
import java.util.UUID;

/** Tenant-wide dashboard: window totals, per-campaign rollup, lead funnel. */
public record DashboardResponse(
        java.time.LocalDate from,
        java.time.LocalDate to,
        MetricTotals totals,
        List<CampaignRollup> campaigns,
        LeadFunnel leads) {

    public record CampaignRollup(UUID campaignId, String name, MetricTotals totals) {}

    /** Lead pipeline snapshot (NEW → CONTACTED → QUALIFIED → CONVERTED / LOST). */
    public record LeadFunnel(long newCount, long contacted, long qualified, long converted, long lost) {}
}
