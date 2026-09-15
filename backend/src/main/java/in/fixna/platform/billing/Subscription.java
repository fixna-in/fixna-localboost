package in.fixna.platform.billing;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tenant subscription → V6 {@code subscriptions} (one per tenant, UNIQUE). */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /** Resolves the effective plan; unknown codes fail closed to FREE. */
    public PlanCode effectivePlan() {
        if (planCode == null) {
            return PlanCode.FREE;
        }
        try {
            return PlanCode.valueOf(planCode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PlanCode.FREE;
        }
    }
}
