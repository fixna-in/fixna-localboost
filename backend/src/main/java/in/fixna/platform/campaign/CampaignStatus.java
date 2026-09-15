package in.fixna.platform.campaign;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Campaign lifecycle (docs/02-architecture/campaign-lifecycle.md):
 * DRAFT → READY_FOR_REVIEW → APPROVED → QUEUED → CREATING → ACTIVE →
 * PAUSED/COMPLETED, with CREATING → FAILED as the recoverable failure path.
 * Only APPROVED campaigns may execute. Transitions are validated server-side.
 */
public enum CampaignStatus {
    DRAFT,
    READY_FOR_REVIEW,
    APPROVED,
    QUEUED,
    CREATING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    FAILED;

    private static final Map<CampaignStatus, Set<CampaignStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(READY_FOR_REVIEW),
            READY_FOR_REVIEW, EnumSet.of(APPROVED, DRAFT),
            APPROVED, EnumSet.of(QUEUED),
            QUEUED, EnumSet.of(CREATING),
            CREATING, EnumSet.of(ACTIVE, FAILED),
            ACTIVE, EnumSet.of(PAUSED, COMPLETED),
            PAUSED, EnumSet.of(ACTIVE, COMPLETED),
            FAILED, EnumSet.of(QUEUED),
            COMPLETED, EnumSet.noneOf(CampaignStatus.class));

    /** Whether a transition from this status to {@code target} is legal. */
    public boolean canTransitionTo(CampaignStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    /** Launch is safe to (re-)attempt only from these states. */
    public boolean isLaunchable() {
        return this == APPROVED || this == FAILED;
    }

    /** Terminal states — no further transitions allowed. */
    public boolean isTerminal() {
        return this == COMPLETED;
    }
}
