# C8 Dependency Direction Checklist

Rules (enforce on every change):
1. `:app` may depend on any `feature:*` or `core:*` modules.
2. `feature:*` modules may depend on `core:*` modules only.
3. `core:*` modules must NOT depend on `:app` or any `feature:*` module.

How to enforce:
- Before each change, sanity-check new dependencies against the rules above.
- If a change requires breaking a rule, stop and propose a refactor or shared `core:*` abstraction instead.
