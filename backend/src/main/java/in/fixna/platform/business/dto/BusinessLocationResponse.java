package in.fixna.platform.business.dto;

import java.math.BigDecimal;
import java.util.UUID;

import in.fixna.platform.business.BusinessLocation;

/** Location response DTO. */
public record BusinessLocationResponse(
        UUID id,
        UUID businessId,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude) {

    public static BusinessLocationResponse from(BusinessLocation location) {
        return new BusinessLocationResponse(
                location.getId(),
                location.getBusinessId(),
                location.getAddressLine(),
                location.getCity(),
                location.getState(),
                location.getPostalCode(),
                location.getCountry(),
                location.getLatitude(),
                location.getLongitude());
    }
}
