package in.fixna.platform.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, UUID> {

    List<BusinessLocation> findByTenantIdAndBusinessId(UUID tenantId, UUID businessId);

    Optional<BusinessLocation> findByIdAndTenantIdAndBusinessId(UUID id, UUID tenantId, UUID businessId);
}
