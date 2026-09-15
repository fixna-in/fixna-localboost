package in.fixna.platform.creative;

/**
 * Creative draft lifecycle. DRAFT is editable; READY means the user reviewed
 * and approved the content for launch. Content can always return to DRAFT
 * while the creative is not referenced by an active execution.
 */
public enum CreativeStatus {
    DRAFT,
    READY;

    /** Allowed status changes (DRAFT→READY, READY→DRAFT, same status no-op). */
    public boolean canTransitionTo(CreativeStatus target) {
        return target != null && target != this;
    }
}