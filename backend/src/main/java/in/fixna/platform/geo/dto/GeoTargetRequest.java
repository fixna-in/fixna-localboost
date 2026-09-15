package in.fixna.platform.geo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import in.fixna.platform.geo.GeoTargetType;

/** Geo target payload. Exactly one targeting dimension must be provided. */
public record GeoTargetRequest(
        @NotNull GeoTargetType targetType,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusKm,
        String countryCode,
        String regionCode,
        String city,
        String postalCode) {}
