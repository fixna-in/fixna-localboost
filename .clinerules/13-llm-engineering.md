# LLM Engineering Rules

Provider abstraction:
AIProvider -> MockAIProvider / OpenAIProvider / AnthropicProvider / GeminiProvider.

Every LLM request:
- has a versioned prompt
- has a structured response schema
- validates JSON/schema
- validates business rules
- records usage/cost metadata
- has timeout/error handling

Do not parse arbitrary natural language into executable application state.
