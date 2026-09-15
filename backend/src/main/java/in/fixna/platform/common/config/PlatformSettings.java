package in.fixna.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed platform settings bound from {@code fixna.platform.*} (Workflow 13).
 * Keeps environment behavior in configuration: mock adapters stay the default
 * for local/test, with endpoints/keys per real provider configurable later.
 */
@ConfigurationProperties(prefix = "fixna.platform")
public record PlatformSettings(String defaultMode) {

    public PlatformSettings {
        if (defaultMode == null || defaultMode.isBlank()) {
            defaultMode = "mock";
        }
    }
}
