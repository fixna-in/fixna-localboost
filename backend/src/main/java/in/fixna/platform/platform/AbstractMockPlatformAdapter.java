package in.fixna.platform.platform;

import java.time.OffsetDateTime;

/**
 * Shared deterministic behaviour for demo-mode adapters (ADR-004): no
 * credentials, no network. External campaign ids are derived from the
 * idempotency key so repeated launches return the same identity, and the
 * campaign name acts as a deterministic failure trigger for tests:
 * a name containing "fail" simulates a transient outage (retryable), a
 * blank name simulates a rejected payload (permanent).
 */
public abstract class AbstractMockPlatformAdapter implements AdvertisingPlatformAdapter {

    private final String platform;
    private final String idPrefix;

    protected AbstractMockPlatformAdapter(String platform, String idPrefix) {
        this.platform = platform;
        this.idPrefix = idPrefix;
    }

    @Override
    public final String platform() {
        return platform;
    }

    @Override
    public final LaunchReceipt launch(PlatformLaunchRequest request) {
        if (request.campaignName() == null || request.campaignName().isBlank()) {
            throw PlatformException.permanent(platform(), "launch", "Campaign name is required");
        }
        if (request.campaignName().toLowerCase().contains("fail")) {
            throw PlatformException.retryable(
                    platform(), "launch", "Simulated transient provider outage");
        }
        return new LaunchReceipt(
                platform(),
                idPrefix + "-" + request.externalReference(),
                "ACTIVE",
                OffsetDateTime.now());
    }
}
