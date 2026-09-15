package in.fixna.platform.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Workflow 13 — prod profile fails fast on localhost/default data-plane config. */
class ProdEnvironmentValidatorTest {

    private ProdEnvironmentValidator validator(MockEnvironment env) {
        return new ProdEnvironmentValidator(env);
    }

    private MockEnvironment prod() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        return env;
    }

    private MockEnvironment prodWith(String url, String username, String password) {
        MockEnvironment env = prod();
        env.setProperty("spring.datasource.url", url);
        env.setProperty("spring.datasource.username", username);
        env.setProperty("spring.datasource.password", password);
        return env;
    }

    @Test
    void nonProdProfileIsNeverChecked() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");

        assertThatCode(() -> validator(env).run(null)).doesNotThrowAnyException();
    }

    @Test
    void prodRefusesLocalhostDefaultDatasource() {
        MockEnvironment env = prodWith(
                "jdbc:postgresql://localhost:5432/fixna", "fixna", "s3cret-in-prod");

        assertThatThrownBy(() -> validator(env).run(null))
                .hasMessageContaining("localhost");
    }

    @Test
    void prodRefusesPlaceholderPassword() {
        MockEnvironment env = prodWith(
                "jdbc:postgresql://db.example.com:5432/fixna", "fixna", "change-me");

        assertThatThrownBy(() -> validator(env).run(null))
                .hasMessageContaining("POSTGRES_PASSWORD");
    }

    @Test
    void prodAcceptsExplicitExternalDatasource() {
        MockEnvironment env = prodWith(
                "jdbc:postgresql://db.example.com:5432/fixna", "fixna", "a-real-prod-secret");
        env.setProperty("spring.data.redis.url", "redis://cache.example.com:6379");

        assertThatCode(() -> validator(env).run(null)).doesNotThrowAnyException();
    }
}