package in.fixna.platform.platform;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Resolves the adapter for a channel name. Core code never touches concrete
 * mock/provider classes — only this registry (ADR-004 dependency direction).
 */
@Component
public class PlatformAdapterRegistry {

    private static final Map<String, String> CHANNEL_ALIASES = Map.of(
            "GOOGLE_ADS", "GOOGLE",
            "META_ADS", "META");

    private final Map<String, AdvertisingPlatformAdapter> byPlatform;

    public PlatformAdapterRegistry(java.util.List<AdvertisingPlatformAdapter> adapters) {
        this.byPlatform = adapters.stream()
                .collect(Collectors.toUnmodifiableMap(
                        a -> a.platform().toUpperCase(Locale.ROOT), Function.identity()));
    }

    public boolean supports(String platform) {
        return platform != null && byPlatform.containsKey(normalize(platform));
    }

    /** Adapter lookup; unknown channels surface as permanent PlatformException. */
    public AdvertisingPlatformAdapter forChannel(String channel) {
        AdvertisingPlatformAdapter adapter = byPlatform.get(normalize(channel));
        if (adapter == null) {
            throw PlatformException.permanent(
                    normalize(channel), "resolve", "No adapter registered for platform " + channel);
        }
        return adapter;
    }

    public Set<String> platforms() {
        return byPlatform.keySet();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return CHANNEL_ALIASES.getOrDefault(upper, upper);
    }
}
