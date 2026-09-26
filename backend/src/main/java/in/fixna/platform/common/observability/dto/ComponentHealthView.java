package in.fixna.platform.common.observability.dto;

import java.util.Map;


public record ComponentHealthView(String status, Map<String, Object> details) {
}
