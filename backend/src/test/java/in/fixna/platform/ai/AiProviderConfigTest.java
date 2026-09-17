package in.fixna.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import in.fixna.platform.common.config.AiSettings;

/**
 * Provider factory honors fixna.ai.provider (WF13): mock default,
 * case-insensitive, unknown providers fail fast.
 */
class AiProviderConfigTest {

    @Test
    void mockIsTheDefaultProvider() {
        AiProviderConfig config = new AiProviderConfig(new AiSettings("mock", 50));
        assertThat(config.aiProvider()).isInstanceOf(MockAIProvider.class);
    }

    @Test
    void providerNameIsCaseInsensitive() {
        AiProviderConfig config = new AiProviderConfig(new AiSettings("MOCK", 50));
        assertThat(config.aiProvider()).isInstanceOf(MockAIProvider.class);
    }

    @Test
    void unknownProviderFailsFast() {
        AiProviderConfig config = new AiProviderConfig(new AiSettings("openai", 50));
        assertThatThrownBy(config::aiProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openai");
    }
}
