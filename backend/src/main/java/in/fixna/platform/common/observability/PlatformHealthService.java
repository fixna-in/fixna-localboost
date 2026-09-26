package in.fixna.platform.common.observability;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import in.fixna.platform.common.observability.dto.ComponentHealthView;
import in.fixna.platform.common.observability.dto.PlatformHealthResponse;

/** Builds aggregated health snapshots from Actuator contributors plus release metadata. */
@Service
public class PlatformHealthService {

    private final HealthEndpoint healthEndpoint;
    private final Optional<BuildProperties> buildProperties;
    private final String appEnv;
    private final String serviceName;
    private final String deployedAtOverride;

    public PlatformHealthService(
            HealthEndpoint healthEndpoint,
            Optional<BuildProperties> buildProperties,
            @Value("${fixna.app-env:local}") String appEnv,
            @Value("${spring.application.name:fixna-api}") String serviceName,
            @Value("${fixna.deployed-at:}") String deployedAtOverride) {
        this.healthEndpoint = healthEndpoint;
        this.buildProperties = buildProperties;
        this.appEnv = appEnv;
        this.serviceName = serviceName;
        this.deployedAtOverride = deployedAtOverride;
    }

    public PlatformHealthResponse snapshot() {
        HealthComponent root = healthEndpoint.health();
        return new PlatformHealthResponse(
                root.getStatus().getCode(),
                resolveVersion(),
                resolveDeployedAt(),
                appEnv,
                serviceName,
                OffsetDateTime.now(),
                extractComponents(root));
    }

    public boolean isHealthy() {
        return Status.UP.equals(healthEndpoint.health().getStatus());
    }

    private Map<String, ComponentHealthView> extractComponents(HealthComponent root) {
        if (!(root instanceof CompositeHealth composite)) {
            return Map.of();
        }
        Map<String, ComponentHealthView> components = new LinkedHashMap<>();
        composite.getComponents().forEach((name, component) ->
                components.put(name, toView(component)));
        return components;
    }

    private static ComponentHealthView toView(HealthComponent component) {
        Map<String, Object> details = component instanceof Health health
                ? Map.copyOf(health.getDetails())
                : Map.of();
        return new ComponentHealthView(component.getStatus().getCode(), details);
    }

    private String resolveVersion() {
        return buildProperties.map(BuildProperties::getVersion).orElse("unknown");
    }

    private String resolveDeployedAt() {
        if (deployedAtOverride != null && !deployedAtOverride.isBlank()) {
            return deployedAtOverride.trim();
        }
        return buildProperties.map(bp -> bp.getTime().toString()).orElse(null);
    }
}
