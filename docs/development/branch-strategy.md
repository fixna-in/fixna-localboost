# Fixna LocalBoost Branch Strategy

## Branches

### `main`
Production-ready code only.

- Protected branch.
- Merge through pull request.
- CI must pass before merge.
- Avoid direct feature development.

### `develop`
Integration branch for completed feature work.

- CI runs on every push and pull request.
- Feature branches merge here first when multiple features are being developed.

### Feature branches

Use:

`feature/<short-description>`

Examples:

- `feature/tenant-management`
- `feature/campaign-draft`
- `feature/ai-recommendation`
- `feature/google-ads-adapter`

### Bug fixes

Use:

`fix/<short-description>`

Examples:

- `fix/campaign-validation`
- `fix/frontend-build`

### Documentation/chore

Use:

`docs/<short-description>`
or
`chore/<short-description>`

## Recommended MVP workflow

For a solo developer using Cursor:

```text
feature/xyz
    |
    | local development
    | Cursor IDE
    | Maven / npm validation
    v
  push
    |
    v
Pull Request -> develop
    |
    v
GitHub Actions CI
    |
    v
merge
    |
    v
develop
    |
    | milestone validation
    v
Pull Request -> main
    |
    v
GitHub Actions CI
    |
    v
main
```

For very small personal changes, you may work directly on `develop`, but feature branches are preferred.

## Commit convention

Use concise conventional-style commits:

- `feat: add tenant management`
- `fix: correct campaign validation`
- `refactor: simplify platform adapter`
- `test: add campaign service tests`
- `docs: update architecture`
- `chore: update dependencies`
