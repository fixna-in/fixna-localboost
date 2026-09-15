package in.fixna.platform.business;

import java.math.BigDecimal;
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

/** Physical location of a business. Verifies parent business ownership. */
@Entity
@Table(name = "business_locations")
@Getter
@Setter
@NoArgsConstructor
public class BusinessLocation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "address_line", length = 500)
    private String addressLine;

    @Column(length = 150)
    private String city;

    @Column(length = 150)
    private String state;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(length = 100)
    private String country = "India";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (country == null) {
            country = "India";
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
