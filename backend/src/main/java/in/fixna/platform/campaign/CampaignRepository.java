package in.fixna.platform.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByTenantId(UUID tenantId);

    List<Campaign> findByTenantIdAndBusinessId(UUID tenantId, UUID businessId);

    Optional<Campaign> findByIdAndTenantId(UUID id, UUID tenantId);
}
