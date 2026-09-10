package in.fixna.platform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bootstrap smoke test (Workflow 00).
 *
 * <p>Runs without any infrastructure (no Postgres/Redis) so {@code mvn test}
 * stays green before Docker is available. A full {@code @SpringBootTest}
 * context test will be added in Workflow 01 once the datasource is reachable.
 */
class FixnaApplicationTest {

    @Test
    void applicationClassLoads() {
        assertThat(FixnaApplication.class).isNotNull();
        assertThat(FixnaApplication.class.getPackageName()).isEqualTo("in.fixna.platform");
    }
}
