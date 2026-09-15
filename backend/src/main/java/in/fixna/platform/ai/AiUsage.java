package in.fixna.platform.ai;

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

/**
 * AI usage/cost tracking (rule 11: track provider, model, prompt version,
 * tokens and estimated cost). Written to the V8 {@code ai_usage_log} table —
 * distinct from V6's {@code ai_usage} which belongs to the recommendation
 * history owned elsewhere.
 */
@Entity
@Table(name = "ai_usage_log")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recommendation_type", nullable = false, length = 50)
    private String recommendationType;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "estimated_cost_usd", precision = 10, scale = 6)
    private java.math.BigDecimal estimatedCostUsd;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
