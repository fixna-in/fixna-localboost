package in.fixna.platform.common.observability;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlywayHealthIndicatorTest {

    @Test
    void upWhenNoPendingMigrations() {
        MigrationInfoService info = mock(MigrationInfoService.class);
        MigrationInfo current = mock(MigrationInfo.class);
        when(current.getVersion()).thenReturn(MigrationVersion.fromVersion("9"));
        when(info.current()).thenReturn(current);
        when(info.applied()).thenReturn(new MigrationInfo[] {current});
        when(info.pending()).thenReturn(new MigrationInfo[] {});

        Flyway flyway = mock(Flyway.class);
        when(flyway.info()).thenReturn(info);

        Health health = new FlywayHealthIndicator(flyway).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("pending", 0);
        assertThat(health.getDetails()).containsEntry("currentVersion", "9");
    }

    @Test
    void downWhenPendingMigrationsExist() {
        MigrationInfoService info = mock(MigrationInfoService.class);
        when(info.current()).thenReturn(null);
        when(info.applied()).thenReturn(new MigrationInfo[] {});
        when(info.pending()).thenReturn(new MigrationInfo[] {mock(MigrationInfo.class)});

        Flyway flyway = mock(Flyway.class);
        when(flyway.info()).thenReturn(info);

        Health health = new FlywayHealthIndicator(flyway).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("pending", 1);
    }
}
