package in.fixna.platform.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findByUserId(UUID userId);

    Optional<TenantMembership> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    List<TenantMembership> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);
}
