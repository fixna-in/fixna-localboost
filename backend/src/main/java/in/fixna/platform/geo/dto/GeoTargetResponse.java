package in.fixna.platform.geo.dto;

import java.math.BigDecimal;
import java.util.UUID;

import in.fixna.platform.geo.GeoTarget;
import in.fixna.platform.geo.GeoTargetType;

/** Geo target response DTO. Tenant id stays server-side. */
public record GeoTargetResponse(
        UUID id,
        UUID campaignId,
        GeoTargetType targetType,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusKm,
        String countryCode,
        String regionCode,
        String city,
        String postalCode) {

    public static GeoTargetResponse from(GeoTarget target) {
        return new GeoTargetResponse(
                target.getId(),
                target.getCampaignId(),
                target.getTargetType(),
                target.getName(),
                target.getLatitude(),
                target.getLongitude(),
                target.getRadiusKm(),
                target.getCountryCode(),
                target.getRegionCode(),
                target.getCity(),
                target.getPostalCode());
    }
}
