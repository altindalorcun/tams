---
name: git-push
description: Stages all changes, generates a Conventional Commits message by analyzing the git diff, commits, and pushes to the remote. Use when the user wants to commit and push changes, or invokes /git-push.
disable-model-invocation: true
---

# git-push

Stages all changes, writes a Conventional Commits message, commits, and pushes.

## Workflow

1. Run `git status` to confirm there are changes to commit. If the working tree is clean, tell the user and stop.
2. Run `git diff HEAD` (and `git diff --cached` if there are already staged changes) to understand what changed.
3. Determine the commit message following the **Conventional Commits** format below.
4. Run `git add -A` to stage all changes.
5. Run `git commit -m "..."` with the generated message (use a HEREDOC for multi-line messages).
6. Run `git push` (use `git push -u origin HEAD` if there is no upstream yet).
7. Report the final commit hash and the remote URL to the user.

## Commit message format

```
<type>(<scope>): <short summary>

[optional body — wrap at 72 chars, explain WHY not WHAT]

[optional footer — BREAKING CHANGE: …  |  Closes #123]
```

### Types

| Type | When to use |
|------|-------------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `refactor` | Code restructuring without behaviour change |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `style` | Formatting, whitespace, no logic change |
| `chore` | Build system, dependencies, tooling |
| `perf` | Performance improvement |
| `ci` | CI/CD pipeline changes |
| `revert` | Reverting a previous commit |

### Scope

Use the service or layer name that changed, e.g. `auth-service`, `parser-service`, `frontend`, `infra`, `docker`, `db`. Omit if the change is truly cross-cutting.

### Rules

- Summary line: ≤ 72 characters, imperative mood, no period at the end.
- Body: explain *why*, not *what* the code does. Omit if the summary is self-explanatory.
- `BREAKING CHANGE:` footer is mandatory when public APIs or contracts change.
- Never include commented-out code or "WIP" in the message.

## Examples

```
feat(auth-service): add JWT refresh-token endpoint
```

```
fix(parser-service): handle malformed PDF gracefully

PyMuPDF raises an exception on encrypted files; catch it and
return a structured error instead of crashing the worker.
```

```
chore(infra): upgrade Postgres image to 16-alpine
```

```
feat(frontend)!: replace REST polling with WebSocket feed

BREAKING CHANGE: clients must reconnect after upgrade; the
/api/v1/status polling endpoint is removed.
```

## Safety rules

- Never force-push to `main` or `master`.
- Never use `--no-verify` to skip hooks.
- If the push is rejected (non-fast-forward), report the error to the user and stop — do not rebase or reset automatically.
