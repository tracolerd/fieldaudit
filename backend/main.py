from threading import Lock

from fastapi import FastAPI, File, HTTPException, UploadFile

from backend.schemas import InspectionSyncRequest, InspectionSyncResponse

app = FastAPI(title="FieldAudit Backend", version="1.0.0")

# Prototype-only in-memory store; records are lost on restart.
# Durable persistence must replace this before production use.
server_inspection_store: dict[str, dict[str, object]] = {}
server_inspection_store_lock = Lock()


@app.get("/")
def read_root() -> dict[str, str]:
    return {"status": "FieldAudit Backend is running with revision-aware sync"}


@app.post("/api/v1/inspections/sync", response_model=InspectionSyncResponse)
def sync_inspection(
    payload: InspectionSyncRequest,
) -> InspectionSyncResponse:
    """
    Apply a versioned client update with retry-safe semantics.

    Equal client versions are accepted only as exact replays. A different
    payload for the same client version or an older client version conflicts.
    A higher client version creates one new server revision.
    """
    with server_inspection_store_lock:
        existing_record = server_inspection_store.get(payload.id)

        if existing_record:
            stored_local_version = int(existing_record["local_version"])
            server_version = int(existing_record["server_version"])

            if payload.local_version < stored_local_version:
                raise HTTPException(
                    status_code=409,
                    detail=(
                        f"Stale local revision {payload.local_version}; "
                        f"latest accepted local revision is {stored_local_version}."
                    ),
                )

            if payload.local_version == stored_local_version:
                same_payload = (
                    payload.title == existing_record["title"]
                    and payload.status == existing_record["status"]
                )
                if not same_payload:
                    raise HTTPException(
                        status_code=409,
                        detail=(
                            "The local revision was already accepted with different "
                            "content. Increment local_version before sending edits."
                        ),
                    )
                saved = existing_record
                message = "Duplicate sync acknowledged; no new server revision created."
            else:
                existing_record["title"] = payload.title
                existing_record["status"] = payload.status
                existing_record["local_version"] = payload.local_version
                existing_record["server_version"] = server_version + 1
                saved = existing_record
                message = "New local revision synchronized."
        else:
            saved = {
                "id": payload.id,
                "title": payload.title,
                "status": payload.status,
                "local_version": payload.local_version,
                "server_version": 1,
            }
            server_inspection_store[payload.id] = saved
            message = "Initial inspection revision synchronized."

        return InspectionSyncResponse(
            id=str(saved["id"]),
            title=str(saved["title"]),
            status="SYNCED",
            server_version=int(saved["server_version"]),
            message=message,
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
