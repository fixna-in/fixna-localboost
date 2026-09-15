package in.fixna.platform.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Daily performance rollup for a campaign (V5 {@code campaign_metrics}).
 * The UNIQUE(campaign_id, metric_date) constraint makes ingest an idempotent
 * upsert — re-seeding a day overwrites instead of duplicating.
 */
@Entity
@Table(
        name = "campaign_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "metric_date"}))
@Getter
@Setter
@NoArgsConstructor
public class CampaignMetric {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal spend = BigDecimal.ZERO;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long reach;

    @Column(nullable = false)
    private long clicks;

    @Column(nullable = false)
    private long conversions;

    @Column(nullable = false)
    private long leads;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (spend == null) {
            spend = BigDecimal.ZERO;
        }
        createdAt = OffsetDateTime.now();
    }
}
