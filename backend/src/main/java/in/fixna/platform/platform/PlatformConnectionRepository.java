package in.fixna.platform.platform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Tenant-scoped connection lookups — never by id alone. */
public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, UUID> {

    List<PlatformConnection> findByTenantId(UUID tenantId);

    Optional<PlatformConnection> findFirstByTenantIdAndPlatformOrderByCreatedAtDesc(
            UUID tenantId, String platform);
}
