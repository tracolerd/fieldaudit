<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:15182D,50:4F46E5,100:0B1220&height=190&section=header&text=FieldAudit&fontSize=42&fontColor=F1F5F9&fontAlignY=38&animation=fadeIn&desc=Offline-first%20inspection%20prototype%20with%20a%20production-readiness%20roadmap&descAlignY=61&descSize=15" alt="FieldAudit project banner" width="100%" />
</p>

<h1 align="center">FieldAudit</h1>
<p align="center">
  <img src="https://img.shields.io/badge/Stage-Prototype%20%7C%20Hardening%20Required-EA5808" alt="Prototype - hardening required" />
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Backend-FastAPI-009688?logo=fastapi&logoColor=white" alt="FastAPI" />
  <img src="https://img.shields.io/badge/License-MIT-64748B" alt="MIT license" />
</p>

**FieldAudit is an offline-first inspection app prototype for creating local inspection drafts, with a Kotlin/Android client and a FastAPI backend scaffold.**

The intended product helps field teams record inspections when connectivity is unreliable, then synchronize data safely when a network becomes available. The repository has moved beyond planning into an initial codebase, but synchronization, backend persistence, attachment handling, security controls, and automated verification are **not production-complete**.

> [!CAUTION]
> **Do not deploy this snapshot or use it for real inspection data.** The backend currently contains a Python syntax error; the Android sync worker does not perform a network sync; server state is in memory; authentication is absent; and Room is configured for destructive fallback migrations that can erase local records. These are implementation blockers, not future enhancements.

## Current implementation

| Area | What exists | Verified limitation |
|---|---|---|
| Android application | Kotlin, Jetpack Compose, basic inspection screen | Creates titled drafts; no complete checklist or inspection lifecycle yet |
| Local persistence | Room database, inspection entity, DAO, repository, Flow-based list | Destructive fallback migrations are enabled and schema export is disabled; upgrade safety is unproven |
| Background sync | WorkManager, Room pending queue, Ktor client and status policy | Sync now sends pending drafts, retries network/5xx failures and marks HTTP 409 conflicts; durable server storage/authentication remain incomplete |
| API contract | Pydantic models and FastAPI sync route scaffold | Backend source does not currently parse and is not production-safe |
| Revision/conflict handling | Initial server-version fields and a 409 branch | Version comparison is not a sound optimistic-concurrency protocol; repeated same-ID requests increment revisions instead of being idempotent |
| Evidence attachments | Room entity and upload route scaffold | Endpoint does not store uploaded bytes but can return UPLOADED |
| Automated checks | GitHub Actions Android build + unit tests; backend pytest workflow | Android CI verifies compilation/unit tests, but emulator-backed end-to-end synchronization is not yet exercised |

The phase checklist in [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) is not proof that every phase is functionally complete. Several items are scaffolds only; the checklist must be reconciled with executable behavior and tests.

## Architecture

The intended architecture is a local-first Android client backed by durable server storage and retryable synchronization. This is the **target direction**, not a claim that the full path works today.

~~~mermaid
flowchart TD
    A[Jetpack Compose UI] --> B[ViewModel and Repository]
    B --> C[Room local database]
    C --> D[Durable pending-operation queue]
    D --> E[WorkManager]
    E --> F[Authenticated HTTPS API]
    F --> G[Persistent server database]
    G --> H[Revision validation and conflict response]
    H --> C
~~~

## Critical findings to close

These findings come from a static review of the default branch on **26 October 2026**. Re-audit the source after fixes.

1. **Backend cannot start as committed.** [backend/main.py](backend/main.py) has the attachment import appended on the same line as the previous function's closing parenthesis, causing a Python syntax error.
2. **The Android sync path is a no-op.** [SyncWorker.kt](app/src/main/java/com/tracolerd/fieldaudit/data/SyncWorker.kt) returns success without reading pending inspections or sending HTTP requests. No scheduling/enqueue path was found, and [AndroidManifest.xml](app/src/main/AndroidManifest.xml) does not declare the Internet permission.
3. **Server state is volatile.** [backend/main.py](backend/main.py) uses a process-local dictionary instead of a database. Data disappears on restart and cannot support reliable multi-instance behavior.
4. **Retry handling is not truly idempotent.** The sync route increments server_version on repeated requests with the same record ID. Comparing local_version to server_version does not correctly represent the client's expected base revision. Define and test explicit idempotency and concurrency semantics.
5. **Authentication and production boundaries are missing.** There is no demonstrated user authentication/authorization, record ownership, tenant isolation, rate limiting, deployment configuration, or enforced HTTPS policy. Do not expose the API publicly in this state.
6. **Attachment handling falsely reports success.** The endpoint returns the supplied filename and UPLOADED status without reading, validating, or storing bytes. python-multipart is absent from requirements despite the UploadFile route.
7. **Local data can be deleted during schema changes.** [AppDatabase.kt](app/src/main/java/com/tracolerd/fieldaudit/data/AppDatabase.kt) enables fallbackToDestructiveMigration(). Add explicit, tested Room migrations and recovery behavior before real users enter data.
8. **Verification is missing.** No automated tests or CI workflow are present. Remove checked-in Python __pycache__ bytecode, repair .gitignore, and reconcile project-state claims with working behavior.
9. **Android release configuration needs review.** The app sets targetSdk to 34. For Google Play submissions, the published requirement starting **31 August 2026** is Android 16 / API 36 or higher for new apps and updates. Validate the current requirement and update the target SDK before store release ([official Android guidance](https://developer.android.com/google/play/requirements/target-sdk)).

## Production-readiness acceptance criteria

Do not mark the MVP production-ready until the following are implemented and evidenced:

- A documented sync protocol with durable pending operations, stable idempotency keys, explicit server/base revisions, structured conflicts, bounded retry with backoff, and process-death recovery.
- Persistent server storage with migrations, constraints, transactions, concurrency-safe writes, and tested backup/restore.
- Authenticated and authorized API access; strict input validation and request/file limits; safe errors; deployment-managed secrets; HTTPS-only production transport; privacy-aware logs.
- Real attachment storage with type/size validation, generated storage keys, ownership checks, safe retrieval, and retention/cleanup rules.
- Non-destructive Room migrations with tests proving drafts and pending operations survive app upgrades and restarts.
- Python unit/API/integration tests; Android unit, Room migration, and instrumentation tests; offline/online, duplicate/retry, and conflict scenarios; CI for build, tests, lint, dependency vulnerabilities, and secret exposure.
- Repeatable deployment with health/readiness checks, environment documentation, monitoring, rollback, and tested database recovery.
- Documentation claims backed by code and passing tests.

A successful compile alone does not prove production readiness. Keep the release claim blocked until security, persistence, sync, migration, and recovery criteria pass.

## Technology

| Component | Current repository state |
|---|---|
| Mobile | Native Android, Kotlin, Jetpack Compose |
| Local database | Room / SQLite |
| Background processing | WorkManager scaffold |
| HTTP client | Ktor scaffold |
| Backend | Python / FastAPI / Pydantic |
| Server persistence | **Not implemented** — production needs a persistent relational database |
| Tests and CI | **Not present in this snapshot** |

The pinned Android and Python dependencies are an initial baseline, not a claim that they are the newest or most secure compatible versions. Review release notes, supported runtimes, and vulnerability advisories before upgrading; prefer compatible, tested updates over unreviewed version bumps.

## Repository layout

- **app/** — Android application, Room entities/DAO/repository, ViewModel, and sync-worker scaffold.
- **backend/** — FastAPI routes and Pydantic schemas.
- **docs/DECISIONS.md** — initial architecture decisions.
- **docs/PROJECT_STATE.md** — phase checklist that needs reconciliation with working functionality.

## License

MIT. See [LICENSE](LICENSE).

---

## Creator, Developer & Maintainer

**Created, designed, developed, and maintained by [Nurul Faiyaz](https://github.com/tracolerd).**

<p align="center">
  <a href="https://www.facebook.com/faiyaz.888"><img src="https://img.shields.io/badge/Facebook-1877F2?style=for-the-badge&logo=facebook&logoColor=white" alt="Facebook" /></a>
  <a href="https://www.instagram.com/tracolerd"><img src="https://img.shields.io/badge/Instagram-E4405F?style=for-the-badge&logo=instagram&logoColor=white" alt="Instagram" /></a>
  <a href="https://www.linkedin.com/in/faiyaz-xyz"><img src="https://img.shields.io/badge/LinkedIn-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white" alt="LinkedIn" /></a>
  <a href="https://x.com/faiyaz_explores"><img src="https://img.shields.io/badge/X-000000?style=for-the-badge&logo=x&logoColor=white" alt="X" /></a>
  <a href="https://github.com/tracolerd"><img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub" /></a>
  <a href="mailto:scrollfaiyaz@gmail.com"><img src="https://img.shields.io/badge/Email-D14836?style=for-the-badge&logo=gmail&logoColor=white" alt="Email" /></a>
  <a href="https://nurul-faiyaz.netlify.app/"><img src="https://img.shields.io/badge/Portfolio-Visit%20Website-0F766E?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Personal website" /></a>
</p>

<p align="center">
  <a href="https://www.facebook.com/faiyaz.888">Facebook</a> ·
  <a href="https://www.instagram.com/tracolerd">Instagram</a> ·
  <a href="https://www.linkedin.com/in/faiyaz-xyz">LinkedIn</a> ·
  <a href="https://x.com/faiyaz_explores">X / Twitter</a> ·
  <a href="https://github.com/tracolerd">GitHub</a> ·
  <a href="mailto:scrollfaiyaz@gmail.com">Email</a> ·
  <a href="https://nurul-faiyaz.netlify.app/">Website</a>
</p>


## Local Android synchronization

The Android worker now sends records in `PENDING_SYNC` to `/api/v1/inspections/sync` when a network is available. For an Android emulator with the FastAPI backend running on the development host, the debug build defaults to `http://10.0.2.2:8000`. Plain HTTP is permitted only through the debug manifest overlay; the release manifest does not enable cleartext traffic.

Configure a real HTTPS API origin for a release build with:

```powershell
./gradlew assembleRelease -PfieldAuditApiBaseUrl=https://your-api.example
```

The current backend stores records in process memory and does not have authentication, so do not expose it to real inspection data or an untrusted network. HTTP 409 responses are marked `CONFLICT`; the app does not yet fetch a server snapshot to offer full merge resolution.
