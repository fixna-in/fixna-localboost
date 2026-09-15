package in.fixna.platform.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Metric ingest payload. Upsert key is (campaign, metricDate). */
public record MetricUpsertRequest(
        @NotNull LocalDate metricDate,
        @NotNull @DecimalMin("0.0") BigDecimal spend,
        long impressions,
        long reach,
        long clicks,
        long conversions,
        long leads) {}
