package in.fixna.platform.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of campaign metrics (timeline point). */
public record MetricPoint(
        LocalDate metricDate,
        BigDecimal spend,
        long impressions,
        long reach,
        long clicks,
        long conversions,
        long leads) {

    public static MetricPoint from(in.fixna.platform.analytics.CampaignMetric metric) {
        return new MetricPoint(
                metric.getMetricDate(),
                metric.getSpend(),
                metric.getImpressions(),
                metric.getReach(),
                metric.getClicks(),
                metric.getConversions(),
                metric.getLeads());
    }
}
