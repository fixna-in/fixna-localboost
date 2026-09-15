package in.fixna.platform.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the active {@link AIProvider}. Mock first (ADR-010); real providers
 * (OpenAI/Anthropic/Gemini) plug in here later without touching call sites.
 */
@Configuration
public class AiProviderConfig {

    @Bean
    public AIProvider aiProvider() {
        return new MockAIProvider();
    }
}
