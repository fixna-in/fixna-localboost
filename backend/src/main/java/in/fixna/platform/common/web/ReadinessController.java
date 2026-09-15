package in.fixna.platform.common.web;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Readiness probe for load-balancer/downtime-safe rollout checks. Returns 200
 * only after the Spring context is fully ready; until then 503 with a
 * {@code DOWN} body. Component-level details remain on Actuator
 * ({@code /actuator/health}).
 */
@RestController
@RequestMapping("/api/v1/health")
public class ReadinessController {

    private final RunnableIsReady isReady;
    private final String appName;

    public ReadinessController(RunnableIsReady isReady,
            @org.springframework.beans.factory.annotation.Value("${spring.application.name:fixna-api}")
            String appName) {
        this.isReady = isReady;
        this.appName = appName;
    }

    @Operation(summary = "Readiness probe — 200 when the app is ready to serve traffic")
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        if (isReady.get()) {
            return ResponseEntity.ok(Map.of(
                    "status", "READY",
                    "service", appName,
                    "timestamp", OffsetDateTime.now().toString()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "DOWN",
                "service", appName,
                "timestamp", OffsetDateTime.now().toString()));
    }

    /** Isolated seam for tests: TRUE only after context readiness. */
    public interface RunnableIsReady {
        boolean get();
    }
}