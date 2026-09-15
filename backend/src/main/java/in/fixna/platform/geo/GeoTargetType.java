package in.fixna.platform.geo;

/**
 * Geo targeting dimension (docs/05-geo-audience + US-007/US-008).
 * Exactly one dimension must be populated per target — enforced by
 * {@link GeoTargetService}.
 */
public enum GeoTargetType {
    /** Circular area around a point (requires latitude + longitude + radiusKm). */
    RADIUS,
    /** Whole city (requires city). */
    CITY,
    /** Postal / PIN code (requires postalCode). */
    POSTAL,
    /** Administrative region/state (requires regionCode). */
    REGION,
    /** Country (requires countryCode). */
    COUNTRY
}
