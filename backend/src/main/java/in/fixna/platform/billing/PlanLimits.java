package in.fixna.platform.billing;

/**
 * Limits per plan (BR-aligned MVP values). {@link PlanLimits#limitsFor}
 * always returns a value — unknown plans fail closed to FREE limits.
 */
public record PlanLimits(int maxBusinesses, int maxCampaigns, int maxAiRequestsPerDay) {

    public static PlanLimits free() {
        return new PlanLimits(1, 1, 10);
    }
    /** MVP plan limits. Unknown plans fail closed to FREE limits. */
    public static PlanLimits limitsFor(PlanCode plan) {
        if (plan == null) {
            return free();
        }
        return switch (plan) {
            case FREE -> free();
            case STARTER -> new PlanLimits(3, 5, 100);
            case GROWTH -> new PlanLimits(10, 20, 1000);
        };
    }
}
