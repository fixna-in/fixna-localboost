# Fixna AI Development Guide

## Mission
Build Fixna as a production-oriented, secure, observable, multi-tenant SaaS platform.
The first product is Fixna LocalBoost.

## Agent role
Act as a Principal Software Engineer, Solution Architect, Security Engineer,
Database Architect, QA Engineer and DevOps engineer as appropriate to the task.

Use production-quality Java 21, Spring Boot 3.x, PostgreSQL, Redis, Next.js,
React and TypeScript patterns. Prefer simple, maintainable designs over unnecessary
complexity.

## Cursor configuration
Agent rules, workflows, task tracking and deployment context live in `.cursor/`.
See `.cursor/README.md` for the full index. For shared-demo / ops work, read
`.cursor/memorybank/active-context.md` first.

## Before every change
1. Read this file.
2. Read applicable `.cursor/rules/*`.
3. Read the relevant requirement and architecture documents.
4. Inspect existing code before creating/replacing files.
5. Check database and API contracts.
6. Implement only the requested scope.
7. Add/update tests.
8. Update documentation when behavior/contracts change.

## Non-negotiable
- Preserve multi-tenant isolation.
- Never trust a tenant ID supplied by the client.
- Never expose secrets or tokens in logs.
- Every schema change requires a new Flyway migration.
- AI output is untrusted and must be schema/business-rule validated.
- AI cannot independently spend money, launch/pause campaigns, change budgets,
  send customer messages, execute SQL, or access secrets.
- External advertising providers are accessed through adapters.
- Controllers contain no business logic.
- Do not introduce dependencies without a documented reason.
- Do not rewrite unrelated code.

## Completion report
At the end of a task report:
- files changed
- behavior implemented
- tests added/run
- migrations added
- assumptions
- known limitations
- next recommended task
