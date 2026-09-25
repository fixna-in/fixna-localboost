# AI Flow

User campaign data
 -> input business validation
 -> tenant quota check
 -> prompt builder
 -> AIProvider
 -> structured JSON
 -> JSON schema validation
 -> output business rule validation
 -> platform compatibility validation
 -> recommendation + usage/audit
 -> user approval
 -> deterministic execution (campaign module)

AI has no direct authority over money, credentials or side effects.

Implementation: `AiRecommendationService` with `AiSchemaValidator`,
`AiBusinessValidator` (input + output) and `AiPlatformCompatibilityValidator`.
Details: `docs/06-ai/ai-guardrails.md`.
