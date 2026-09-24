package in.fixna.platform.common.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Local-only fixture bootstrap. ApplicationRunner executes after Flyway/JPA startup. */
@Component
@Profile("local & !prod & !staging")
@ConditionalOnProperty(name = "fixna.test-data.enabled", havingValue = "true")
public class LocalTestDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTestDataSeeder.class);
    private final NamedParameterJdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public LocalTestDataSeeder(NamedParameterJdbcTemplate jdbc, PasswordEncoder passwordEncoder,
            Environment environment) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Fail closed even if a deployment combines profiles or overrides the enable flag.
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length != 1 || Arrays.stream(profiles)
                .anyMatch(p -> !p.equals("local"))
                || !"local".equals(environment.getProperty("fixna.app-env", "local"))) {
            throw new IllegalStateException("Test data seeding requires only the local profile");
        }
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE email = :email)",
                Map.of("email", "owner@example.com"), Boolean.class);
        if (Boolean.TRUE.equals(exists)) {
            LOG.info("operation=local_test_seed status=skipped reason=account_exists");
            return;
        }
        String password = environment.getProperty("fixna.test-data.password", "");
        if (password.isBlank()) {
            LOG.warn("operation=local_test_seed status=skipped reason=password_not_configured; "
                    + "set FIXNA_TEST_USER_PASSWORD to enable the local fixture");
            return;
        }
        // BCrypt truncates input beyond 72 bytes; reject rather than silently truncate.
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Local test password must be at least 8 characters and at most 72 UTF-8 bytes");
        }
        String sql = new ClassPathResource("db/seed/local-test-data.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        Long created = jdbc.queryForObject(sql,
                Map.of("passwordHash", passwordEncoder.encode(password)), Long.class);
        LOG.info("operation=local_test_seed status={}", Long.valueOf(1).equals(created) ? "created" : "skipped");
    }
}
