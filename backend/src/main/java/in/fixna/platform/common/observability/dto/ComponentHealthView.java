package in.fixna.platform.common.observability.dto;

import java.util.Map;

/** Per-component health status and optional probe details. */
public record ComponentHealthView(String status, Map<String, Object> details) {
}
