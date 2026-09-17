package in.fixna.platform.ai;

import in.fixna.platform.common.config.AiSettings;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the active {@link AIProvider} from {@code fixna.ai.provider}
 * (ADR-010, Workflow 13). Mock is the only MVP provider; anything else
 * fails fast at startup instead of silently running mocks in production.
 * Real providers (OpenAI/Anthropic/Gemini) plug in here without touching
 * call sites.
 */
@Configuration
public class AiProviderConfig {

    private final AiSettings settings;

    public AiProviderConfig(AiSettings settings) {
        this.settings = settings;
    }

    @Bean
    public AIProvider aiProvider() {
        if (!"mock".equalsIgnoreCase(settings.provider())) {
            throw new IllegalStateException(
                    "Unsupported FIXNA_AI_PROVIDER '" + settings.provider()
                            + "' — only 'mock' is available in the MVP");
        }
        return new MockAIProvider();
    }
}
