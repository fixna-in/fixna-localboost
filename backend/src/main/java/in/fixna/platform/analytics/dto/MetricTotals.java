package in.fixna.platform.analytics.dto;

import java.math.BigDecimal;

/** Additive totals shared by timeline and dashboard responses. */
public record MetricTotals(
        BigDecimal spend,
        long impressions,
        long reach,
        long clicks,
        long conversions,
        long leads) {

    public static MetricTotals zero() {
        return new MetricTotals(BigDecimal.ZERO, 0, 0, 0, 0, 0);
    }
}
