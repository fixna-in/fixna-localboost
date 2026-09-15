package in.fixna.platform.audience;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AudienceRepository extends JpaRepository<Audience, UUID> {

    List<Audience> findByTenantIdAndCampaignId(UUID tenantId, UUID campaignId);

    Optional<Audience> findByIdAndTenantId(UUID id, UUID tenantId);
}