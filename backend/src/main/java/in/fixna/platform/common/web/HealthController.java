package in.fixna.platform.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.common.observability.PlatformHealthService;
import in.fixna.platform.common.observability.dto.PlatformHealthResponse;
import io.swagger.v3.oas.annotations.Operation;

/**
 * Aggregated platform health for operators and the versioned public API.
 * Per-component probes also surface on {@code /actuator/health} when details are enabled.
 * Flyway migration health is provided by Spring Boot Actuator (not a custom bean).
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final PlatformHealthService platformHealthService;

    public HealthController(PlatformHealthService platformHealthService) {
        this.platformHealthService = platformHealthService;
    }

    @Operation(summary = "Aggregated health — overall status, components, version, deployedAt")
    @GetMapping
    public ResponseEntity<PlatformHealthResponse> health() {
        PlatformHealthResponse body = platformHealthService.snapshot();
        HttpStatus status = platformHealthService.isHealthy()
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }
}
