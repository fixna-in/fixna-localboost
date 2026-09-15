package in.fixna.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed AI settings bound from {@code fixna.ai.*} (Workflow 13).
 * The daily quota is the configured ceiling; per-plan enforcement stays in
 * PlanLimitChecker (subscription limits are the operative gate).
 */
@ConfigurationProperties(prefix = "fixna.ai")
public record AiSettings(String provider, int dailyQuota) {

    public AiSettings {
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if (dailyQuota <= 0) {
            dailyQuota = 50;
        }
    }
}
