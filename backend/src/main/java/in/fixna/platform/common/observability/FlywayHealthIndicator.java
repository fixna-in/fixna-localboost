package in.fixna.platform.common.observability;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Reports Flyway migration state on the health endpoint. */
@Component("flyway")
public class FlywayHealthIndicator implements HealthIndicator {

    private final Flyway flyway;

    public FlywayHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        int pending = info.pending().length;
        Health.Builder builder = pending == 0 ? Health.up() : Health.down();
        builder
                .withDetail("applied", info.applied().length)
                .withDetail("pending", pending);
        if (current != null && current.getVersion() != null) {
            builder.withDetail("currentVersion", current.getVersion().getVersion());
        }
        return builder.build();
    }
}
