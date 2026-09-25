# AI Recommendations API (Workflow 06)

Base: /api/v1/ai (Bearer required; tenant scope from JWT)

## POST /ai/recommendations
Request:
```json
{
  "type": "CAMPAIGN_STRATEGY | AUDIENCE | BUDGET_ALLOCATION | CREATIVE",
  "businessId": "uuid (optional)",
  "campaignId": "uuid (optional)",
  "payload": { "budget": 5000, "channels": [{"channel": "META", "amount": 3000}] }
}
```
Response 200:
```json
{
  "provider": "mock", "model": "mock-gpt-demo", "promptVersion": "mock-v1",
  "data": { "...type-specific validated structure..." },
  "promptTokens": 120, "completionTokens": 180, "estimatedCostMicros": 90
}
```

## Errors
- 400 `AI_VALIDATION_FAILED` — request payload violates business rules (BR-4 shadow check, audience bounds).
- 502 `AI_RESPONSE_INVALID` — provider output failed structural schema validation (AI output is untrusted).
- 502 `AI_OUTPUT_BUSINESS_INVALID` — provider output failed business rules (e.g. audience bounds, duplicate allocation channels).
- 502 `AI_PLATFORM_INCOMPATIBLE` — output references a platform/channel with no registered adapter.
- 502 `AI_PROVIDER_FAILED` — provider failure.

## Safety (per AI rules)
- Advisory only: never mutates campaign state, budgets, or platform adapters.
- Usage/cost recorded per call in `ai_usage_log` (migration V8) with tenant scope.
- Provider behind `AIProvider` abstraction; `MockAIProvider` default (`fixna.ai.provider=mock`).
