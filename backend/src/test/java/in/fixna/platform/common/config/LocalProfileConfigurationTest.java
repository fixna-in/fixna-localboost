package in.fixna.platform.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProfileConfigurationTest {
    @Test
    void localUsesPersistentPostgresWithoutAutomaticFixtures() throws Exception {
        var properties = new YamlPropertySourceLoader()
                .load("local", new ClassPathResource("application-local.yml")).getFirst();
        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5432/localboost");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("postgres");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${POSTGRES_PASSWORD}");
        assertThat(properties.getProperty("fixna.test-data.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("spring.autoconfigure.exclude[0]"))
                .isEqualTo("org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
        assertThat(new ClassPathResource("application-local-pg.yml").exists()).isFalse();
        assertThat(new ClassPathResource("application-local-nodocker.yml").exists()).isFalse();
        assertThat(getClass().getClassLoader().getResource(
                "in/fixna/platform/common/config/EmbeddedPostgresConfig.class")).isNull();
    }
}
