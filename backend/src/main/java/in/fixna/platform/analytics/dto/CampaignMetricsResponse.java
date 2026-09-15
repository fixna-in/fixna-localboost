package in.fixna.platform.analytics.dto;

import java.time.LocalDate;
import java.util.List;

import in.fixna.platform.analytics.CampaignMetric;

/** Campaign-scoped analytics: window totals plus the daily timeline. */
public record CampaignMetricsResponse(
        java.util.UUID campaignId,
        String campaignName,
        LocalDate from,
        LocalDate to,
        MetricTotals totals,
        List<MetricPoint> timeline) {

    public static CampaignMetricsResponse of(
            java.util.UUID campaignId,
            String campaignName,
            LocalDate from,
            LocalDate to,
            MetricTotals totals,
            List<CampaignMetric> metrics) {
        return new CampaignMetricsResponse(
                campaignId,
                campaignName,
                from,
                to,
                totals,
                metrics.stream().map(MetricPoint::from).toList());
    }
}
