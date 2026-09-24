package in.fixna.platform.common.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LocalTestDataSeederTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final BCryptPasswordEncoder encoder = spy(new BCryptPasswordEncoder());

    private MockEnvironment local() {
        return new MockEnvironment().withProperty("fixna.app-env", "local")
                .withProperty("fixna.test-data.password", "LocalFixture-123!");
    }

    @Test
    void refusesNonLocalEnvironmentBeforeDatabaseAccess() {
        MockEnvironment env = local().withProperty("fixna.app-env", "prod");
        env.setActiveProfiles("local");
        assertThatThrownBy(() -> new LocalTestDataSeeder(jdbc, encoder, env).run(null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jdbc, encoder);
    }

    @Test
    void refusesMixedProfilesBeforeDatabaseAccess() {
        MockEnvironment env = local();
        env.setActiveProfiles("local", "prod");
        assertThatThrownBy(() -> new LocalTestDataSeeder(jdbc, encoder, env).run(null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jdbc, encoder);
    }

    @Test
    void existingAccountIsNeverModifiedOrRehashed() throws Exception {
        MockEnvironment env = local();
        env.setActiveProfiles("local");
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(true);
        new LocalTestDataSeeder(jdbc, encoder, env).run(null);
        verify(jdbc).queryForObject(anyString(), eq(Map.of("email", "owner@example.com")), eq(Boolean.class));
        verifyNoMoreInteractions(jdbc);
        verifyNoInteractions(encoder);
    }

    @Test
    void missingPasswordSkipsCreation() throws Exception {
        MockEnvironment env = local().withProperty("fixna.test-data.password", "");
        env.setActiveProfiles("local");
        new LocalTestDataSeeder(jdbc, encoder, env).run(null);
        verify(jdbc).queryForObject(anyString(), anyMap(), eq(Boolean.class));
        verifyNoMoreInteractions(jdbc);
        verifyNoInteractions(encoder);
    }

    @Test
    void persistentLocalProfileSeedsWithEncodedPassword() throws Exception {
        MockEnvironment env = local();
        env.setActiveProfiles("local");
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(false);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(1L);
        new LocalTestDataSeeder(jdbc, encoder, env).run(null);
        verify(encoder).encode("LocalFixture-123!");
        verify(jdbc).queryForObject(anyString(), org.mockito.ArgumentMatchers.<Map<String, ?>>argThat(
                parameters -> parameters.size() == 1
                        && encoder.matches("LocalFixture-123!", (String) parameters.get("passwordHash"))), eq(Long.class));
    }

    @Test
    void persistentProfilePreservesExistingAccount() throws Exception {
        MockEnvironment env = local();
        env.setActiveProfiles("local");
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(true);
        new LocalTestDataSeeder(jdbc, encoder, env).run(null);
        verify(jdbc).queryForObject(anyString(), anyMap(), eq(Boolean.class));
        verifyNoMoreInteractions(jdbc);
        verifyNoInteractions(encoder);
    }

    @Test
    void rejectsCombinedDatabaseProfiles() {
        MockEnvironment env = local();
        env.setActiveProfiles("local", "dev");
        assertThatThrownBy(() -> new LocalTestDataSeeder(jdbc, encoder, env).run(null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jdbc, encoder);
    }

    @Test
    void rejectsPersistentProfileInProduction() {
        MockEnvironment env = local().withProperty("fixna.app-env", "prod");
        env.setActiveProfiles("local");
        assertThatThrownBy(() -> new LocalTestDataSeeder(jdbc, encoder, env).run(null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jdbc, encoder);
    }

    @Test
    void rejectsShortAndOverlongBcryptPasswords() {
        for (String password : new String[] {"short", "x".repeat(73)}) {
            MockEnvironment env = local().withProperty("fixna.test-data.password", password);
            env.setActiveProfiles("local");
            assertThatThrownBy(() -> new LocalTestDataSeeder(jdbc, encoder, env).run(null))
                    .isInstanceOf(IllegalStateException.class);
        }
        verifyNoInteractions(encoder);
    }
}
