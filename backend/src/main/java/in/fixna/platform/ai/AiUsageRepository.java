package in.fixna.platform.ai;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    List<AiUsage> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Count of usage rows since a cutoff (AI quota window). */
    long countByTenantIdAndCreatedAtAfter(UUID tenantId, OffsetDateTime createdAt);
}
