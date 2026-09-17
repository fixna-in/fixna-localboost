package in.fixna.platform.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Typed settings records: safe defaults when env config is absent (WF13). */
class SettingsDefaultsTest {

    @Test
    void aiSettingsDefaultToMockWithSaneQuota() {
        AiSettings settings = new AiSettings(null, 0);
        assertThat(settings.provider()).isEqualTo("mock");
        assertThat(settings.dailyQuota()).isEqualTo(50);
    }

    @Test
    void platformSettingsDefaultToMock() {
        PlatformSettings settings = new PlatformSettings(" ");
        assertThat(settings.defaultMode()).isEqualTo("mock");
    }
}
