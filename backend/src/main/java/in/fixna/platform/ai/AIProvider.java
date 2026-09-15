package in.fixna.platform.ai;

/**
 * Provider abstraction for AI recommendations (ADR-003, rules 11/13).
 * Implementations turn a structured, tenant-scoped request into a structured
 * result carrying prompt/model/version and usage metadata. Providers are
 * advisory-only: they can never mutate campaign state, budgets or launch
 * anything, and their output is untrusted until validated.
 */
public interface AIProvider {

    /** Stable provider identifier (e.g. {@code mock}) for audit/usage rows. */
    String name();

    /** Model identifier used for usage/cost tracking. */
    String model();

    /** Generates a structured recommendation for the request. */
    RecommendationResult generate(RecommendationRequest request);
}
