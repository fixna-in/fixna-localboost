package in.fixna.platform.geo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Geographic target attached to a campaign (V4 geo_targets). */
@Entity
@Table(name = "geo_targets")
@Getter
@Setter
@NoArgsConstructor
public class GeoTarget {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private GeoTargetType targetType;

    @Column(length = 255)
    private String name;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "radius_km", precision = 8, scale = 2)
    private BigDecimal radiusKm;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "region_code", length = 100)
    private String regionCode;

    @Column(length = 150)
    private String city;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

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
