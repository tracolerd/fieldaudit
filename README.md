<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:15182D,50:4F46E5,100:0B1220&height=190&section=header&text=FieldAudit&fontSize=42&fontColor=F1F5F9&fontAlignY=38&animation=fadeIn&desc=Planned%20offline-first%20Android%20inspections%20and%20resilient%20synchronization&descAlignY=61&descSize=15" alt="FieldAudit animated project banner" width="100%" />
</p>
<h1 align="center">FieldAudit</h1>
<p align="center"><img src="https://img.shields.io/badge/Stage-Planning-8B5CF6" alt="Planning stage" /></p>


**A native Android inspection workflow that remains useful without a network connection.**

FieldAudit is a planned offline-first mobile application for conducting structured inspections of assets, facilities, or equipment. Users should be able to create and complete inspections locally, then synchronize changes with a server when connectivity returns.

> **Project status:** Initial planning and repository setup. The Android app, backend, local persistence, and synchronization protocol are not implemented yet.

## Problem

Field inspections frequently take place in areas with unreliable connectivity. A mobile app that requires a constant connection risks losing work or blocking the user at the moment an inspection is needed. Offline support introduces its own challenges: durable local data, retryable synchronization, duplicate requests, and conflicting edits.

FieldAudit is intended to address these problems explicitly rather than treating offline mode as an afterthought.

## Planned MVP

- View available inspection templates.
- Create an inspection and save progress locally.
- Complete checklist items with notes and a clear workflow state.
- Persist drafts and completed records on-device.
- Show connection and pending-sync status.
- Queue changes for background synchronization.
- Retry failed sync operations without losing local records.
- Detect revision conflicts and present a user-visible resolution path.
- Keep a traceable history of important synchronization outcomes.

Photo evidence and broader device permissions are possible later additions, not MVP requirements.

## Proposed architecture

~~~mermaid
flowchart TD
    A[Android UI] --> B[Domain and Repository Layer]
    B --> C[Room Local Database]
    C --> D[Pending Sync Queue]
    D --> E[WorkManager]
    E --> F[REST API]
    F --> G[Server Database]
    G --> H[Revision and Conflict Response]
    H --> C
~~~

The local database is the source of truth for the user's current offline work. The sync layer should communicate explicit server revisions and idempotency identifiers so that retries do not silently duplicate or overwrite records.

## Proposed technology

| Area | Planned choice |
|---|---|
| Platform | Native Android |
| Language | Kotlin |
| Local persistence | Room over SQLite |
| Background work | WorkManager |
| Backend | Python and FastAPI |
| API | REST with documented request/response contracts |
| Server database | PostgreSQL or another documented relational store |
| Testing | Unit tests, Android instrumentation tests, and backend API tests |

Firebase is not required for the initial release. It may be evaluated later for a concrete need such as push notifications, but the main synchronization protocol should remain understandable and testable independently.

## Synchronization principles

- Give each locally created record a stable UUID.
- Record local changes durably before attempting the network request.
- Make sync requests safe to retry through idempotency keys or stable operation identifiers.
- Use server revisions or explicit version fields to detect concurrent changes.
- Do not silently apply last-write-wins when two users changed the same record.
- Expose conflicts so a user can review and resolve them.
- Preserve unsynchronized local data when the server is unavailable.
- Make sync state visible and recoverable after application restarts.

## Development roadmap

1. Establish the Android project and define the inspection data model.
2. Add Room entities, DAO tests, and a repository layer.
3. Build the primary inspection and checklist screens.
4. Add a small FastAPI backend with a documented contract.
5. Implement the local operation queue and idempotent sync endpoint.
6. Add WorkManager scheduling, retry behavior, and clear sync status.
7. Implement revision conflicts and user-visible resolution.
8. Test offline/online transitions, restarts, and failure recovery.
9. Document build requirements, emulator steps, and release considerations.

## Testing strategy

- Local create/edit/complete workflows without network access.
- App restart while drafts and pending sync records exist.
- Server unavailable, timeout, and retry scenarios.
- Duplicate sync requests and idempotency behavior.
- Concurrent edits producing a conflict.
- Invalid server responses and schema-version mismatches.
- Room database migration tests as the schema evolves.
- Backend authorization and input validation tests.
- Emulator testing with connectivity disabled and restored.

## Security and privacy

Inspection records may contain location or operational details. Collect only necessary data, minimize permissions, and document retention. Use authenticated API access, protect tokens using Android-supported secure storage practices, and avoid writing secrets or sensitive inspection details to diagnostic logs.

## Current status

The repository currently contains the project brief and MIT license only. There is no installable application, verified offline workflow, backend endpoint, or synchronization implementation yet.

## Author & Development

**Project author and solo developer:** [Nurul Faiyaz](https://github.com/tracolerd).

This project is independently developed and maintained by Nurul Faiyaz. AI-assisted tools may be used as part of the development workflow; the project author remains responsible for technical decisions, code review, validation, and maintenance.

## License

MIT. See [LICENSE](LICENSE).

