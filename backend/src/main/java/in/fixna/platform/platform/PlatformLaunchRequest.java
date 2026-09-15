package in.fixna.platform.platform;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Provider-neutral launch input for one channel allocation.
 * {@code externalReference} is the idempotency key (BR-6): adapters must map
 * it 1:1 to the external campaign id so retries never duplicate spend.
 */
public record PlatformLaunchRequest(
        UUID tenantId,
        UUID campaignId,
        String externalReference,
        String campaignName,
        String objective,
        String channel,
        BigDecimal budget,
        String currency,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String connectionAccountId) {}
