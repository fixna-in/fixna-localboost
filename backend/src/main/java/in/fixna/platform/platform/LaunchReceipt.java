package in.fixna.platform.platform;

import java.time.OffsetDateTime;

/** What the platform reported back after a launch attempt. */
public record LaunchReceipt(
        String platform,
        String externalCampaignId,
        String status,
        OffsetDateTime launchedAt) {}
