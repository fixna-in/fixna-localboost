package in.fixna.platform.creative.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import in.fixna.platform.creative.CreativeStatus;

/**
 * Create/update creative payload. No tenant field — scope from JWT. At least
 * one of headline/body/callToAction must be present (service-side check).
 */
public record CreativeRequest(
        @NotBlank @Size(max = 30) String channel,
        @Size(max = 500) String headline,
        String body,
        @Size(max = 100) String callToAction,
        CreativeStatus status) {}