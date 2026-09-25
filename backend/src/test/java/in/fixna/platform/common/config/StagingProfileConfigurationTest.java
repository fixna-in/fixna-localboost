package in.fixna.platform.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class StagingProfileConfigurationTest {

    @Test
    void stagingDisablesRedisAndTestDataSeed() throws Exception {
        var properties = new YamlPropertySourceLoader()
                .load("staging", new ClassPathResource("application-staging.yml")).getFirst();
        assertThat(properties.getProperty("fixna.test-data.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("fixna.app-env")).isEqualTo("demo");
        assertThat((String) properties.getProperty("spring.autoconfigure.exclude[0]"))
                .contains("RedisAutoConfiguration");
    }
}
