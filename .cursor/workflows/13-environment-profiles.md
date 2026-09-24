# Workflow 13 - Environment Profile Configuration

## Objective
Implement profile-based configuration for Fixna LocalBoost so the same application artifact can run across local, dev, test, staging, and production without code changes.

## Backend profiles

Create:

```text
backend/src/main/resources/
├── application.yml
├── application-local.yml
├── application-dev.yml
├── application-test.yml
├── application-staging.yml
└── application-prod.yml
```

`application.yml` contains only safe/common defaults. Profile files contain environment-specific values such as database, Redis, service URLs, feature flags, connection limits, and log levels.

Use Spring Boot profiles and typed `@ConfigurationProperties` for grouped application settings.

Example:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## Secrets

Never commit passwords, API keys, OAuth secrets, JWT signing secrets, cloud credentials, or advertising-platform credentials.

Use environment variables or the deployment platform's secret manager.

Production must fail fast when mandatory configuration is missing and must never silently fall back to local/dev values.

## Local and test

Local uses persistent PostgreSQL at localhost:5432/localboost (postgres user; POSTGRES_PASSWORD supplied at runtime), without Redis or embedded PostgreSQL:

```bash
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

Tests must use the `test` profile where appropriate:

```java
@ActiveProfiles("test")
```

Tests must never accidentally connect to dev or production.

## Frontend

Use `.env.example` for documented configuration and `.env.local` for local values.

Use `NEXT_PUBLIC_*` only for values intentionally exposed to browsers. Never put secrets in `NEXT_PUBLIC_*`.

Example:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_ENV=local
```

## Rules

1. Do not scatter environment checks through business code.
2. Keep environment-specific behavior in configuration, adapters, infrastructure, or feature flags.
3. Keep committed configuration safe.
4. Document non-obvious properties.
5. Keep provider endpoints configurable so mock adapters work locally.
6. CI must not depend on developer-specific configuration.

## Validation

Run:

```bash
mvn -f backend/pom.xml test
```

and:

```bash
cd frontend
npm run build
```

Verify the active profile at startup and verify no secrets are committed.

## Acceptance criteria

- local/dev/test/staging/prod profiles exist.
- Secrets are externalized.
- Test configuration is isolated.
- Local development works with PostgreSQL installed on localhost.
- Frontend environment configuration is documented.
- CI remains environment-independent.
