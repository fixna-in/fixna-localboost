# AI Guardrails

Fixna LocalBoost treats GenAI as **advisory only**. Recommendations never mutate
campaign state, budgets, credentials or platform connections. Execution happens
only after explicit user approval through separate modules.

## Recommendation pipeline

```
request payload
  -> input business validation (AiBusinessValidator.validate)
  -> tenant quota check
  -> AIProvider.generate
  -> schema validation (AiSchemaValidator)
  -> output business validation (AiBusinessValidator.validateOutput)
  -> platform compatibility (AiPlatformCompatibilityValidator)
  -> usage + audit
  -> RecommendationResult (for user approval)
```

Human approval and deterministic launch execution live in the campaign module
(ADR-005). AI output is never passed directly to adapters.

## Deterministic layers

| Layer | Component | Purpose |
|-------|-----------|---------|
| Demo provider | `MockAIProvider` | Same request → same JSON and token/cost metadata (ADR-010) |
| Validation | Schema + business validators | Pure Java rules; no LLM in the gate |
| Post-approval | Mock platform adapters | Deterministic external ids from `external_reference` |

Real LLM providers are non-deterministic by nature. Guardrails ensure only
structured, rule-compliant, platform-compatible output reaches the UI.

## Module boundaries

The `ai` package must not depend on execution services:

- `CampaignLaunchService` / `CampaignLaunchTx`
- `CampaignService`
- `PlatformConnectionService`
- `NotificationService`
- `BillingService` / `SubscriptionService`

Platform references in AI output are checked through `PlatformAdapterRegistry`
(read-only lookup). AI code must not import concrete adapter classes.

`NonNegotiablesComplianceTest` enforces these boundaries at build time.

## Error codes

| Code | HTTP | When |
|------|------|------|
| `AI_VALIDATION_FAILED` | 400 | Request payload violates business rules |
| `AI_RESPONSE_INVALID` | 502 | Provider output failed structural schema check |
| `AI_OUTPUT_BUSINESS_INVALID` | 502 | Provider output failed business rules |
| `AI_PLATFORM_INCOMPATIBLE` | 502 | Output references an unsupported platform/channel |
| `AI_PROVIDER_FAILED` | 502 | Provider call failed |

## Platform channel names

Adapters register as `GOOGLE`, `META`, `WHATSAPP`. Campaign DB rows may use
`GOOGLE_ADS` / `META_ADS`; `PlatformAdapterRegistry` normalizes these aliases
before lookup.

## Non-negotiables (summary)

AI cannot:

- spend money
- launch or pause campaigns
- modify budgets
- access credentials
- execute SQL or shell
- send customer messages

See `AGENTS.md`, `.cursor/rules/ai-llm.mdc` and `docs/06-ai/ai-safety-rules.md`.
