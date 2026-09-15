package in.fixna.platform.lead;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, LeadStatus status);

    /**
     * Tenant-scoped search with optional business/campaign/status filters;
     * campaign filter is applied only within the given business so parent
     * ownership can never be bypassed by filter combinations.
     */
    @Query("""
            select l from Lead l
            where l.tenantId = :tenantId
              and (:businessId is null or l.businessId = :businessId)
              and (:campaignId is null or l.campaignId = :campaignId)
              and (:status is null or l.status = :status)
            """)
    Page<Lead> search(
            @Param("tenantId") UUID tenantId,
            @Param("businessId") UUID businessId,
            @Param("campaignId") UUID campaignId,
            @Param("status") LeadStatus status,
            Pageable pageable);
}
