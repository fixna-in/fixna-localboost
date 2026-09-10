# Architecture Rules

Use a modular monolith for MVP.

Backend modules:
auth, tenant, user, business, campaign, audience, geo, creative, platform,
analytics, lead, ai, billing, notification, admin, common.

Dependency direction:
Controller -> Application Service -> Domain/Business Logic -> Repository/Infrastructure.

External platforms must use adapters.
AI must use AIProvider abstraction.
Do not make the application dependent on provider SDKs in domain code.
