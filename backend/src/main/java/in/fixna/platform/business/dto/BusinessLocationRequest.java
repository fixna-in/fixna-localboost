package in.fixna.platform.business.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Size;

/** Create/update location payload. */
public record BusinessLocationRequest(
        @Size(max = 500) String addressLine,
        @Size(max = 150) String city,
        @Size(max = 150) String state,
        @Size(max = 30) String postalCode,
        @Size(max = 100) String country,
        BigDecimal latitude,
        BigDecimal longitude) {}
