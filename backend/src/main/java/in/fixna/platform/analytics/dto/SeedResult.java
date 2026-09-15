package in.fixna.platform.analytics.dto;

/** Result of demo metric seeding (idempotent upserts per campaign/day). */
public record SeedResult(int campaigns, int metricDays) {}
