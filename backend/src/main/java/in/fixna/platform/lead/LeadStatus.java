package in.fixna.platform.lead;

/** Lead pipeline stages (BR funnel: NEW → CONTACTED → QUALIFIED → CONVERTED / LOST). */
public enum LeadStatus {
    NEW,
    CONTACTED,
    QUALIFIED,
    CONVERTED,
    LOST;

    /** Terminal stages close the lead; funnel integrity forbids leaving them. */
    public boolean isTerminal() {
        return this == CONVERTED || this == LOST;
    }
}
