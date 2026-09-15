package in.fixna.platform.creative;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreativeRepository extends JpaRepository<Creative, UUID> {

    List<Creative> findByTenantIdAndCampaignId(UUID tenantId, UUID campaignId);

    Optional<Creative> findByIdAndTenantId(UUID id, UUID tenantId);
}