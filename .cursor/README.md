# Cursor Agent Configuration

This directory contains Cursor IDE agent rules, workflows, and task tracking
for the Fixna LocalBoost project.

## Structure

| Path | Purpose |
|------|---------|
| `rules/` | Persistent agent rules (`.mdc` files with YAML frontmatter) |
| `workflows/` | Phase-by-phase implementation workflows (00–14) |
| `tasks/` | Current task status and project backlog |
| `memorybank/` | Live deployment context and project state for agents |

## Getting started

1. Read `AGENTS.md` at the repository root.
2. Cursor automatically loads `rules/` with `alwaysApply: true`.
3. File-specific rules activate when matching files are open (e.g. `backend/**`).
4. Check `tasks/CURRENT-TASK.md` for active work and `tasks/BACKLOG.md` for status.
5. Read `memorybank/active-context.md` for deployment state and current URLs.

## Rules index

| Rule file | Scope |
|-----------|-------|
| `fixna-core.mdc` | Mission, architecture, non-negotiables (always on) |
| `agent-execution.mdc` | Build verification, CI, loop prevention (always on) |
| `security-tenancy.mdc` | Auth, RBAC, multi-tenant isolation (always on) |
| `backend.mdc` | Java/Spring Boot conventions |
| `frontend.mdc` | Next.js/React conventions |
| `database.mdc` | PostgreSQL/Flyway migrations |
| `api.mdc` | REST API and OpenAPI |
| `ai-llm.mdc` | AI advisory flow and LLM engineering |
| `platform-adapters.mdc` | Advertising platform adapters |
| `observability.mdc` | Logging, tracing, metrics |
| `testing.mdc` | Test standards |
| `git.mdc` | Commit and branch conventions |

## Migration note

This project was originally configured for Cline (`.cline/`, `.clinerules/`).
Agent context now lives under `.cursor/` including `memorybank/` for deployment
and session state. Canonical Flyway migrations live in
`backend/src/main/resources/db/migration/`.
