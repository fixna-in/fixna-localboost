package in.fixna.platform.common.web;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.observability.PlatformHealthService;
import in.fixna.platform.common.observability.dto.ComponentHealthView;
import in.fixna.platform.common.observability.dto.PlatformHealthResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void returns200WhenHealthy() {
        PlatformHealthService service = mock(PlatformHealthService.class);
        when(service.isHealthy()).thenReturn(true);
        when(service.snapshot()).thenReturn(sample("UP"));

        HealthController controller = new HealthController(service);
        var response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().version()).isEqualTo("1.0.0");
        assertThat(response.getBody().components()).containsKey("db");
    }

    @Test
    void returns503WhenUnhealthy() {
        PlatformHealthService service = mock(PlatformHealthService.class);
        when(service.isHealthy()).thenReturn(false);
        when(service.snapshot()).thenReturn(sample("DOWN"));

        HealthController controller = new HealthController(service);
        var response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().status()).isEqualTo("DOWN");
    }

    private static PlatformHealthResponse sample(String status) {
        return new PlatformHealthResponse(
                status,
                "1.0.0",
                "2026-09-26T12:00:00Z",
                "demo",
                "fixna-localboost-backend",
                OffsetDateTime.parse("2026-09-26T12:00:00+00:00"),
                Map.of("db", new ComponentHealthView("UP", Map.of("database", "PostgreSQL"))));
    }
}
