package in.fixna.platform.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.PlatformAdapterRegistry;

/**
 * Ensures channel/platform references in untrusted AI output can be fulfilled by
 * registered {@link in.fixna.platform.platform.AdvertisingPlatformAdapter}s
 * (ai-flow: platform compatibility validation). Runs after schema and business
 * validation so only structurally valid payloads are checked.
 */
@Component
public class AiPlatformCompatibilityValidator {

    private final PlatformAdapterRegistry registry;

    public AiPlatformCompatibilityValidator(PlatformAdapterRegistry registry) {
        this.registry = registry;
    }

    /** Throws {@code AI_PLATFORM_INCOMPATIBLE} when output references unknown platforms. */
    public void validate(RecommendationType type, Map<String, Object> data) {
        Map<String, Object> output = data == null ? Map.of() : data;
        List<String> channels = channelsReferenced(type, output);
        List<String> unsupported = new ArrayList<>();
        for (String channel : channels) {
            if (!registry.supports(channel)) {
                unsupported.add(channel);
            }
        }
        if (!unsupported.isEmpty()) {
            throw new FixnaException(
                    "AI_PLATFORM_INCOMPATIBLE", HttpStatus.BAD_GATEWAY,
                    "AI output references unsupported platform(s): " + unsupported
                            + "; supported: " + registry.platforms());
        }
    }

    private List<String> channelsReferenced(RecommendationType type, Map<String, Object> output) {
        return switch (type) {
            case CAMPAIGN_STRATEGY -> stringList(output.get("channels"));
            case BUDGET_ALLOCATION -> allocationChannels(output.get("allocations"));
            case AUDIENCE, CREATIVE -> List.of();
        };
    }

    private List<String> allocationChannels(Object raw) {
        if (!(raw instanceof List<?> allocations)) {
            return List.of();
        }
        List<String> channels = new ArrayList<>();
        for (Object item : allocations) {
            if (item instanceof Map<?, ?> allocation
                    && allocation.get("channel") instanceof String channel
                    && !channel.isBlank()) {
                channels.add(channel);
            }
        }
        return channels;
    }

    private List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> values)) {
            return List.of();
        }
        List<String> channels = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String channel && !channel.isBlank()) {
                channels.add(channel);
            }
        }
        return channels;
    }
}
