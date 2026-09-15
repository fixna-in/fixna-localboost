package in.fixna.platform.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignMetricRepository extends JpaRepository<CampaignMetric, UUID> {

    /** Lookup key for the upsert path — unique per campaign + date. */
    Optional<CampaignMetric> findByTenantIdAndCampaignIdAndMetricDate(
            UUID tenantId, UUID campaignId, LocalDate metricDate);

    List<CampaignMetric> findByTenantIdAndCampaignIdAndMetricDateBetweenOrderByMetricDateAsc(
            UUID tenantId, UUID campaignId, LocalDate from, LocalDate to);

    List<CampaignMetric> findByTenantIdAndMetricDateBetweenOrderByMetricDateAsc(
            UUID tenantId, LocalDate from, LocalDate to);
}
