package in.fixna.platform.common.observability.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Aggregated platform health: overall status, per-component status, and release metadata.
 */
public record PlatformHealthResponse(
        String status,
        String version,
        String deployedAt,
        String environment,
        String service,
        OffsetDateTime timestamp,
        Map<String, ComponentHealthView> components) {
}
