package in.fixna.platform.billing;

/**
 * MVP plan catalog. FREE is the implicit default subscription created lazily
 * for every tenant; no payment provider integration (explicitly excluded).
 */
public enum PlanCode {
    FREE,
    STARTER,
    GROWTH
}
