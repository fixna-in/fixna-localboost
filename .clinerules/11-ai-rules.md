# AI Rules

AI is advisory, never authoritative.

Flow:
input -> provider -> structured JSON -> schema validation -> business validation ->
platform compatibility -> recommendation -> user approval -> execution.

AI must never:
- spend money
- launch/pause campaigns
- modify budgets
- access credentials
- execute SQL/shell
- send customer messages

Use AIProvider abstraction with MockAIProvider for local/demo mode.
Track prompt version, provider/model, token usage and estimated cost.
Apply timeouts, rate limits and tenant quotas.
