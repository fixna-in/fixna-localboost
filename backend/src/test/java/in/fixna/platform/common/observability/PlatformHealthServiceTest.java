package in.fixna.platform.common.observability;


import java.time.Instant;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;

import in.fixna.platform.common.observability.dto.PlatformHealthResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformHealthServiceTest {

    @Test
    void snapshotAggregatesComponentsAndMetadata() {
        CompositeHealth root = mock(CompositeHealth.class);
        when(root.getStatus()).thenReturn(Status.UP);
        when(root.getComponents()).thenReturn(Map.of(
                "db", Health.up().withDetail("database", "PostgreSQL").build(),
                "platform", Health.up().withDetail("version", "1.0.0").build()));
        HealthEndpoint endpoint = mock(HealthEndpoint.class);
        when(endpoint.health()).thenReturn(root);

        Properties props = new Properties();
        props.setProperty("version", "1.0.0");
        props.setProperty("time", "2026-09-26T12:00:00Z");
        props.setProperty("artifact", "fixna-api");
        props.setProperty("group", "in.fixna");
        props.setProperty("name", "fixna-api");
        BuildProperties build = new BuildProperties(props);

        PlatformHealthService service = new PlatformHealthService(
                endpoint,
                Optional.of(build),
                "demo",
                "fixna-localboost-backend",
                "");


        PlatformHealthResponse response = service.snapshot();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.version()).isEqualTo("1.0.0");
        assertThat(response.environment()).isEqualTo("demo");

        assertThat(response.deployedAt()).isEqualTo(Instant.parse("2026-09-26T12:00:00Z").toString());
        assertThat(response.components()).containsKeys("db", "platform");
        assertThat(response.components().get("db").status()).isEqualTo("UP");
        assertThat(service.isHealthy()).isTrue();
    }

    @Test
    void deployedAtOverrideTakesPrecedence() {
        CompositeHealth root = mock(CompositeHealth.class);
        when(root.getStatus()).thenReturn(Status.UP);
        when(root.getComponents()).thenReturn(Map.of());
        HealthEndpoint endpoint = mock(HealthEndpoint.class);
        when(endpoint.health()).thenReturn(root);

        PlatformHealthService service = new PlatformHealthService(
                endpoint, Optional.empty(), "demo", "fixna-localboost-backend", "2026-09-26T18:00:00Z");

        assertThat(service.snapshot().deployedAt()).isEqualTo("2026-09-26T18:00:00Z");
    }
}
