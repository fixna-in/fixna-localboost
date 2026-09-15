package in.fixna.platform.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    List<Business> findByTenantId(UUID tenantId);

    Optional<Business> findByIdAndTenantId(UUID id, UUID tenantId);
}
