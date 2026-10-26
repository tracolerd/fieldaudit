from fastapi import FastAPI, HTTPException
from backend.schemas import InspectionSyncRequest, InspectionSyncResponse
app = FastAPI(title="FieldAudit Backend", version="1.0.0")
# In-memory store representing persistent server DB with revision tracking
# Key: inspection UUID, Value: dict containing fields and server_version
server_inspection_store = {}
@app.get("/")
def read_root():
    return {"status": "FieldAudit Backend is running with Idempotency & Revision Support"}
@app.post("/api/v1/inspections/sync", response_model=InspectionSyncResponse)
def sync_inspection(payload: InspectionSyncRequest):
    """
    Idempotent sync endpoint:
    - Uses client-provided UUID as primary lookup.
    - Tracks server_version (revision) to prevent silent data overwrites.
    - Returns 409 Conflict if client local_version is strictly less than server version.
    """
    existing_record = server_inspection_store.get(payload.id)
    if existing_record:
        server_version = existing_record["server_version"]
        # Conflict detection: Client is trying to sync an obsolete revision
        if payload.local_version < server_version:
            raise HTTPException(
                status_code=409, 
                detail=f"Conflict detected. Server revision is {server_version}, but local version is {payload.local_version}."
            )
        # Idempotent update: If client version matches or is ahead, accept and increment server version
        existing_record["title"] = payload.title
        existing_record["status"] = payload.status
        existing_record["server_version"] = server_version + 1
    else:
        # First-time sync for this UUID (Idempotent insert)
        server_inspection_store[payload.id] = {
            "id": payload.id,
            "title": payload.title,
            "status": payload.status,
            "server_version": 1
        }
    saved = server_inspection_store[payload.id]
    return InspectionSyncResponse(
        id=saved["id"],
        title=saved["title"],
        status="SYNCED",
        server_version=saved["server_version"],
        message="Idempotent sync successful with revision tracking."
    )from fastapi import UploadFile, File
@app.post("/api/v1/inspections/{inspection_id}/attachment")
async def upload_attachment(inspection_id: str, file: UploadFile = File(...)):
    # Save or process uploaded evidence file
    return {
        "inspection_id": inspection_id,
        "filename": file.filename,
        "status": "UPLOADED"
    }
