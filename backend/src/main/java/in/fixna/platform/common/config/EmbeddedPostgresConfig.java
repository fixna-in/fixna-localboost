package in.fixna.platform.common.config;

import java.io.IOException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Workflow 13 / local-no-Docker path: starts a real embedded PostgreSQL
 * (Zonky — native binaries, no Docker) when the {@code local-nodocker}
 * profile is active, and exposes it as the primary {@link DataSource}.
 * Flyway applies the canonical V1–V9 + V100 migrations to it, so the schema
 * is identical to production. Data lives in a temp directory and is wiped on
 * every start ({@code setCleanDataDirectory(true)}) — perfect for demos.
 * The Docker-based `local` profile and every other profile are unaffected.
 *
 * <p>{@code @Primary} is essential: without it a property-driven Hikari pool
 * (the {@code localhost:5432} defaults in application.yml) could win
 * DataSource resolution and fail auth against a locally installed
 * PostgreSQL. The profile additionally excludes
 * {@code DataSourceAutoConfiguration} so no property-driven pool can exist.
 */
@Configuration
@Profile("local-nodocker")
public class EmbeddedPostgresConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
                .setCleanDataDirectory(true)
                .start();
    }

    /** Primary pooled DataSource wrapping the embedded PostgreSQL server. */
    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        HikariDataSource hikari = new HikariDataSource();
        hikari.setDataSource(embeddedPostgres.getPostgresDatabase());
        hikari.setMaximumPoolSize(10);
        hikari.setPoolName("embedded-postgres");
        return hikari;
    }
}

