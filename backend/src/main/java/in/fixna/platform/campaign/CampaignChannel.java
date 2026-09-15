package in.fixna.platform.campaign;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Budget split across advertising channels (V4 campaign_channels, BR-4). */
@Entity
@Table(name = "campaign_channels")
@Getter
@Setter
@NoArgsConstructor
public class CampaignChannel {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(name = "allocated_budget", nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedBudget;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }
}
