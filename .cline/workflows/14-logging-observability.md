# Workflow 14 - Application Logging and Observability

## Objective
Implement consistent logging for Fixna LocalBoost that supports debugging, operations, audit correlation, and future centralized observability.

## Logging rules

1. Use SLF4J (`@Slf4j` or an injected logger).
2. Never use `System.out.println()` or `System.err.println()` in production code.
3. Never log passwords, API keys, tokens, Authorization headers, cookies, or credentials.
4. Do not log full request/response payloads by default.
5. Avoid unnecessary sensitive tenant/user/business data.
6. Use levels correctly:
   - ERROR: failed operation requiring attention
   - WARN: unusual/recoverable condition
   - INFO: important lifecycle/business event
   - DEBUG: developer diagnostics
   - TRACE: very detailed diagnostics
7. Use parameterized logging.
8. Log exception objects to preserve stack traces.

## Correlation context

Support these fields where safely available:

- traceId
- spanId
- requestId
- tenantId
- userId
- campaignId
- operation

If OpenTelemetry is present, prefer trace/span IDs for distributed correlation.

## HTTP request correlation

Implement a safe `X-Request-ID` mechanism:

- accept a safe incoming request ID or generate one
- place it in MDC during request processing
- return it in the response header
- always clear MDC in `finally`

Log request metadata such as:

```text
method=POST path=/api/campaigns status=201 durationMs=84
```

Do not log Authorization headers or sensitive request bodies.

## Business events

Use consistent logs for events such as:

- tenant created
- authentication success/failure
- business created
- campaign created
- campaign submitted for review
- campaign approved
- campaign queued
- platform execution started/completed/failed
- AI recommendation generated
- AI recommendation rejected by validation
- external provider failure

Logging is not a replacement for the database audit trail.

## Multi-tenancy

Include tenant ID when safely available from authenticated tenant context.

Never trust a client-provided tenant ID for authorization. Never expose another tenant's data through logs or errors.

## Global exception handling

The API exception handler must:

1. Log the internal exception with correlation identifiers.
2. Return a safe client-facing error.
3. Never expose stack traces or internal implementation details.
4. Return consistent error codes.

Example:

```java
log.error(
    "Campaign operation failed campaignId={} tenantId={} requestId={}",
    campaignId, tenantId, requestId, ex
);
```

## AI and external platform logging

For AI, log metadata such as provider, model, operation, latency, validation result, and token usage when available.

For Google/Meta/WhatsApp adapters, log adapter, operation, internal campaign ID, provider operation ID when safe, latency, status/category, and success/failure.

Never log prompts/responses or provider payloads when they contain sensitive data, and never log access tokens.

## Configuration

Common logging configuration belongs in `application.yml`; environment-specific levels belong in profile files.

Example:

```yaml
logging:
  level:
    root: INFO
    in.fixna.platform: INFO
```

Local can use DEBUG. Production should normally remain INFO unless targeted diagnostics are intentionally enabled.

Design the logging layer so production can later emit structured JSON without changing business code.

## Validation

Search for forbidden console logging and run:

```bash
mvn -f backend/pom.xml test
```

Also run:

```bash
cd frontend
npm run build
```

Verify request correlation, exception stack traces, safe API errors, tenant correlation, and absence of secrets in logs.

## Acceptance criteria

- Central logging convention exists.
- No `System.out`/`System.err` in backend production code.
- Request/correlation IDs are available.
- Tenant context is safely correlatable.
- Global exceptions are logged without leaking internals.
- Log levels are profile-aware.
- Secrets and sensitive payloads are not logged.
- Logging is compatible with OpenTelemetry and future centralized JSON logging.
