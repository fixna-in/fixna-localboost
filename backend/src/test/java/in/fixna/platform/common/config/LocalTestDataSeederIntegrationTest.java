package in.fixna.platform.common.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class LocalTestDataSeederIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;
    private LocalTestDataSeeder seeder;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("TRUNCATE tenants, users CASCADE");
        var env = new MockEnvironment().withProperty("fixna.app-env", "local")
                .withProperty("fixna.test-data.password", "LocalFixture-123!");
        env.setActiveProfiles("local");
        seeder = new LocalTestDataSeeder(new NamedParameterJdbcTemplate(ds), encoder, env);
    }

    @Test
    void createsLoginCompatibleOwnerAndTenantScopedBusinessExactlyOnce() throws Exception {
        seeder.run(null);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users", String.class);
        assertThat(encoder.matches("LocalFixture-123!", hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT role FROM tenant_memberships", String.class)).isEqualTo("TENANT_OWNER");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM business_locations l
                JOIN businesses b ON b.id = l.business_id AND b.tenant_id = l.tenant_id
                JOIN tenant_memberships m ON m.tenant_id = b.tenant_id
                JOIN users u ON u.id = m.user_id WHERE u.email = 'owner@example.com'
                """, Long.class)).isEqualTo(1L);
        jdbc.update("UPDATE businesses SET name = 'Edited by owner'");
        seeder.run(null);
        for (String table : new String[] {"users", "tenants", "tenant_memberships", "businesses", "business_locations"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(1L);
        }
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users", String.class)).isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT name FROM businesses", String.class)).isEqualTo("Edited by owner");
        assertCompleteFixture();
    }

    private void assertCompleteFixture() {
        var expected = java.util.Map.of("campaigns", 2L, "campaign_offers", 2L,
                "campaign_channels", 4L, "geo_targets", 2L, "audiences", 2L,
                "creatives", 4L, "leads", 10L, "campaign_metrics", 7L, "subscriptions", 1L);
        expected.forEach((table, count) -> {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(count);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table
                    + " WHERE tenant_id NOT IN (SELECT tenant_id FROM tenant_memberships)", Long.class)).isZero();
        });
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM campaigns c WHERE c.total_budget <>
                    (SELECT sum(allocated_budget) FROM campaign_channels ch
                     WHERE ch.campaign_id = c.id AND ch.tenant_id = c.tenant_id)
                """, Long.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM leads l JOIN campaigns c ON c.id = l.campaign_id
                WHERE l.business_id <> c.business_id OR l.tenant_id <> c.tenant_id
                """, Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT plan_code FROM subscriptions", String.class)).isEqualTo("STARTER");
        assertThat(jdbc.queryForObject("SELECT sum(impressions) FROM campaign_metrics", Long.class)).isEqualTo(14000L);
    }

    @Test
    void doesNotAttachExistingUserToNewTenant() throws Exception {
        jdbc.update("INSERT INTO users(email, password_hash) VALUES (?, ?)", "owner@example.com", "existing-hash");
        seeder.run(null);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users", String.class)).isEqualTo("existing-hash");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tenants", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tenant_memberships", Long.class)).isZero();
    }

    @Test
    void sqlItselfIsIdempotentEvenWithoutLoaderPrecheck() throws Exception {
        String sql = new org.springframework.core.io.ClassPathResource("db/seed/local-test-data.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var named = new NamedParameterJdbcTemplate(jdbc);
        var params = java.util.Map.of("passwordHash", encoder.encode("LocalFixture-123!"));
        assertThat(named.queryForObject(sql, params, Long.class)).isEqualTo(1L);
        assertThat(named.queryForObject(sql, params, Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tenants", Long.class)).isEqualTo(1L);
        assertCompleteFixture();
    }
}
