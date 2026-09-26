package in.fixna.platform.common.observability;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/** Exposes release metadata on {@code /actuator/health} under the {@code platform} component. */
@Component("platform")
public class PlatformHealthIndicator implements HealthIndicator {

    private final Optional<BuildProperties> buildProperties;
    private final String appEnv;
    private final String deployedAtOverride;

    public PlatformHealthIndicator(
            Optional<BuildProperties> buildProperties,
            @Value("${fixna.app-env:local}") String appEnv,
            @Value("${fixna.deployed-at:}") String deployedAtOverride) {
        this.buildProperties = buildProperties;
        this.appEnv = appEnv;
        this.deployedAtOverride = deployedAtOverride;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up()
                .withDetail("environment", appEnv)
                .withDetail("version", resolveVersion());
        String deployedAt = resolveDeployedAt();
        if (deployedAt != null) {
            builder.withDetail("deployedAt", deployedAt);
        }
        return builder.build();
    }

    String resolveVersion() {
        return buildProperties.map(BuildProperties::getVersion).orElse("unknown");
    }

    String resolveDeployedAt() {
        if (deployedAtOverride != null && !deployedAtOverride.isBlank()) {
            return deployedAtOverride.trim();
        }
        return buildProperties.map(bp -> bp.getTime().toString()).orElse(null);
    }
}
