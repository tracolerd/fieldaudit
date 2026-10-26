# Prioritized Engineering Backlog

FieldAudit is an offline-first inspection prototype. The following candidates are based on the README's documented blockers and should be verified against current source before implementation.

## Candidate issues

1. **Fix the backend syntax error and add an import/launch smoke test** — Make the FastAPI module parse and boot in a clean Python environment before expanding functionality.
2. **Implement real pending-record synchronization** — Have the Android worker load unsynced Room records, submit them to the configured API, and mark records synced only after a confirmed response.
3. **Replace in-memory server state with durable persistence** — Define migrations and repository boundaries so inspection records survive a backend restart.
4. **Remove destructive Room fallback migrations** — Add schema exports and explicit upgrade migrations; preserve local inspection data during app upgrades.
5. **Define sync idempotency and conflict resolution** — Use stable record identifiers, server versions, retry-safe requests, and a clear policy for concurrent edits.
6. **Add authentication and per-user ownership checks** — Prevent one user from viewing or changing another user's inspection data before connecting real teams.
7. **Validate and bound attachment handling** — Set file size/type limits, safe naming, authorization, and retry behavior for attachment upload/download.
8. **Represent offline, retrying, conflicted, and failed sync states** — Avoid reporting success when no network operation occurred and expose actionable status in the UI.
9. **Add backend and Android automated tests** — Cover validation, migrations, retry idempotency, conflict paths, and offline behavior.
10. **Create CI with reproducible build and test commands** — Parse/lint/test the FastAPI service and run Android unit tests or a documented build verification workflow.

## Suggested order

Item 1 is the immediate blocker. Next implement a real durable, idempotent sync path (2–5); then secure the data and handle attachments (6–8). Do not use this snapshot for real inspections until data-loss and access-control risks are addressed.
