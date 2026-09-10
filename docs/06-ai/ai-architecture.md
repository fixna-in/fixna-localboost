# AI Architecture

AIProvider is the only dependency exposed to application services.

Implement:
MockAIProvider
future OpenAIProvider
future AnthropicProvider
future GeminiProvider

AI outputs use JSON schemas and are validated before application use.
AI usage is attributed to tenant and tracked for quotas/costs.
