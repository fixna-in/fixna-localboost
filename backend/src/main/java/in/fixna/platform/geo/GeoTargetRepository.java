package in.fixna.platform.geo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeoTargetRepository extends JpaRepository<GeoTarget, UUID> {

    List<GeoTarget> findByTenantIdAndCampaignId(UUID tenantId, UUID campaignId);

    Optional<GeoTarget> findByIdAndTenantId(UUID id, UUID tenantId);
}
