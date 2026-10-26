# Architecture Decisions
## 2026-10-26: Initial Setup
- **UI Framework:** Jetpack Compose (Modern native standard).
- **Local Storage:** Room Database (SQLite wrapper).
- **Sync Mechanism:** WorkManager for deferred, guaranteed background execution.
- **Idempotency:** Client-generated stable UUIDs for all sync operations to prevent duplicate records.
- **Development Flow:** Pure PowerShell automation for generation and builds.