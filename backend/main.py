from fastapi import FastAPI, File, HTTPException, UploadFile

from backend.schemas import InspectionSyncRequest, InspectionSyncResponse

app = FastAPI(title="FieldAudit Backend", version="1.0.0")

# In-memory store representing persistent server DB with revision tracking.
# NOTE: This remains prototype-only; data is lost when the process restarts.
# Key: inspection UUID, value: inspection fields and server_version.
server_inspection_store: dict[str, dict[str, object]] = {}


@app.get("/")
def read_root() -> dict[str, str]:
    return {
        "status": "FieldAudit Backend is running with Idempotency & Revision Support"
    }


@app.post("/api/v1/inspections/sync", response_model=InspectionSyncResponse)
def sync_inspection(
    payload: InspectionSyncRequest,
) -> InspectionSyncResponse:
    """
    Prototype sync endpoint.

    Uses a client-provided identifier and a revision counter. This in-memory
    implementation is not a production-ready idempotency or concurrency
    protocol; durable persistence and proper optimistic concurrency remain
    separate follow-up work.
    """
    existing_record = server_inspection_store.get(payload.id)

    if existing_record:
        server_version = int(existing_record["server_version"])

        # Conflict detection: an older local revision cannot overwrite a newer
        # server revision. This is a prototype rule, not a full base-revision
        # protocol.
        if payload.local_version < server_version:
            raise HTTPException(
                status_code=409,
                detail=(
                    f"Conflict detected. Server revision is {server_version}, "
                    f"but local version is {payload.local_version}."
                ),
            )

        # Preserve the existing prototype behavior pending the dedicated
        # idempotency/concurrency fix.
        existing_record["title"] = payload.title
        existing_record["status"] = payload.status
        existing_record["server_version"] = server_version + 1
    else:
        server_inspection_store[payload.id] = {
            "id": payload.id,
            "title": payload.title,
            "status": payload.status,
            "server_version": 1,
        }

    saved = server_inspection_store[payload.id]
    return InspectionSyncResponse(
        id=str(saved["id"]),
        title=str(saved["title"]),
        status="SYNCED",
        server_version=int(saved["server_version"]),
        message="Idempotent sync successful with revision tracking.",
    )


@app.post("/api/v1/inspections/{inspection_id}/attachment")
async def upload_attachment(
    inspection_id: str,
    file: UploadFile = File(...),
) -> dict[str, str | None]:
    # Attachment persistence and validation are not implemented yet. Do not
    # treat this scaffold response as proof that file bytes were stored.
    return {
        "inspection_id": inspection_id,
        "filename": file.filename,
        "status": "NOT_PERSISTED",
    }
