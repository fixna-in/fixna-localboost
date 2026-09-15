# WF06 State Checkpoint — COMPLETE (verified green)

Scope shipped: AIProvider + MockAIProvider (mock-v1), RecommendationType
(STRATEGY/AUDIENCE/BUDGET/CREATIVE), AiSchemaValidator (untrusted output),
AiBusinessValidator (BR-3/BR-4 request-side), AiRecommendationService
(input validation → provider → schema → business → usage row → audit),
AiController POST /api/v1/ai/recommendations, AiProviderConfig (swappable),
AiUsage → ai_usage_log (V8). Advisory-only; never mutates campaign state.

Canonical contract (defined by tests, reconciled 2026-09-11):
- RecommendationRequest.of(type, tenantId, businessId, campaignId, context)
- RecommendationResult(provider, model, promptVersion, data, promptTokens,
  completionTokens, estimatedCostMicros)
- Service ctor: (provider, schemaValidator, businessValidator, usageRepo, audit)
- Errors: AI_VALIDATION_FAILED (input, provider not called),
  AI_RESPONSE_INVALID (schema, no usage row)

(Shipped as originally listed; note the final design persisting usage to
ai_usage_log (V8) with the recommendation history left to V6's owner.
Full suite green 2026-09-11 — 66 tests, 0 failures, BUILD SUCCESS.)
