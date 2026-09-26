package in.fixna.platform.common.observability.dto;

import java.util.Map;

/** Health of a single infrastructure or platform component. */
public record ComponentHealthView(String status, Map<String, Object> details) {
}
