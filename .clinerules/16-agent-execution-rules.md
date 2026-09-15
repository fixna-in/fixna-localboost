# Agent Execution Rules

## Command Execution

After executing a command:

1. Inspect the command result and exit code.
2. If exit code is 0, treat the command as successful.
3. Do not repeatedly read generated log files to determine success.
4. Never perform the same file-read operation more than once unless new information
   indicates the file changed.
5. If a log is needed, read the relevant section once.
6. Prefer terminal output and exit codes over repeatedly reading redirected logs.
7. If a command succeeds, continue with the workflow.
8. If a command fails, diagnose the actual failure before modifying code.

## Loop Prevention

Never repeat an identical tool call more than once.

If the same information has already been retrieved:
- use the existing result
- inspect another relevant file
- run a targeted command
- or ask for guidance

Do not repeatedly read the same file hoping for a different result.

## Build Verification

A build is considered successful when:
- command exits with code 0
- terminal returns to the shell
- no build error is reported

Do not require additional log-file inspection when these conditions are satisfied.


## Cline Agent Execution and CI Rules

## Purpose

Cline must work in a controlled, repeatable way and must treat command exit codes as the primary source of truth.

## Build and validation rules

1. Before changing code, inspect the relevant project files and existing implementation.
2. For backend changes, use Java 21 and Maven.
3. For frontend changes, use the existing Node.js/Next.js project configuration.
4. After a meaningful backend change, run:
   `mvn -f backend/pom.xml test`
5. After a meaningful frontend change, run:
   `npm install` when dependencies changed, then:
   `npm run build`
6. For a full validation before a milestone/PR, run:
   `mvn -f backend/pom.xml test`
   and
   `npm run build` from `frontend`.
7. Treat a command with exit code 0 as successful unless there is direct evidence of a functional problem.
8. If a command succeeds and returns to the shell prompt, do not repeatedly inspect the same generated log file.
9. Do not make the same tool call repeatedly with identical arguments.
10. If a build log is needed, read it once. Prefer terminal output and exit code over repeatedly reading generated logs.
11. If a command fails, inspect the actual error, make the smallest reasonable fix, and rerun the relevant command.
12. Do not change unrelated files merely to make CI green.
13. Do not remove tests, disable validation, or weaken quality gates just to pass CI.
14. Do not claim CI passed unless the relevant local command actually passed or GitHub Actions reports success.

## Git rules

- Work normally on a feature branch.
- Do not commit directly to `main` for feature work.
- Use pull requests for merging feature branches into `develop` or `main`.
- Keep commits small and meaningful.
- Do not commit secrets, API keys, passwords, tokens, `.env` files, generated build artifacts, `node_modules`, or IDE-specific files.

## GitHub Actions awareness

The repository CI workflow is:
`.github/workflows/ci.yml`

Cline should assume GitHub Actions is the independent CI quality gate.

Recommended sequence:

1. Implement change.
2. Run focused local validation.
3. Run full backend/frontend validation when appropriate.
4. Review `git diff`.
5. Check `git status`.
6. Commit only when explicitly asked or when the current workflow requires it.
7. Push only when explicitly asked.
8. After push, use GitHub Actions status as the final remote validation signal.

## Failure recovery

If Cline gets stuck in a repeated read/tool loop:

- Stop repeating the same call.
- Use the last command's exit code and terminal output.
- Inspect `git status` and `git diff`.
- Continue from the next logical workflow step.
- Never manufacture a failure from a successful command.
