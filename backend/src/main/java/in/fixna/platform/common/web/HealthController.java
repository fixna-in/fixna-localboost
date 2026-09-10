package in.fixna.platform.common.web;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Lightweight liveness probe under the versioned API for bootstrap checks.
 * Detailed component health stays on Actuator (/actuator/health).
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String appName;

    public HealthController(@Value("${spring.application.name:fixna-api}") String appName) {
        this.appName = appName;
    }

    @Operation(summary = "API liveness probe")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", appName,
                "timestamp", OffsetDateTime.now().toString()));
    }
}
