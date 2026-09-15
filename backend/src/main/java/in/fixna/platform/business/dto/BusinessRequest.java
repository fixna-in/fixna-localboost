package in.fixna.platform.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update business payload. No tenant field — scope from JWT. */
public record BusinessRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String category,
        String description,
        @Size(max = 1000) String websiteUrl,
        @Size(max = 50) String phone) {}
