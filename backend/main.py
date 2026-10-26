from fastapi import FastAPI, HTTPException
from backend.schemas import InspectionSyncRequest, InspectionSyncResponse
app = FastAPI(title="FieldAudit Backend", version="1.0.0")
# In-memory store for MVP baseline (will map to PostgreSQL/SQLite later)
fake_server_db = {}
@app.get("/")
def read_root():
    return {"status": "FieldAudit Backend is running"}
@app.post("/api/v1/inspections/sync", response_model=InspectionSyncResponse)
def sync_inspection(payload: InspectionSyncRequest):
    # Idempotency & Revision logic scaffold
    existing = fake_server_db.get(payload.id)
    if existing:
        server_version = existing["server_version"]
        # Basic conflict check rule
        if payload.local_version < server_version:
            raise HTTPException(status_code=409, detail="Conflict: Server has a newer revision.")
        # Update existing record
        existing["title"] = payload.title
        existing["status"] = payload.status
        existing["server_version"] += 1
    else:
        # Create new record on server
        server_version = 1
        fake_server_db[payload.id] = {
            "id": payload.id,
            "title": payload.title,
            "status": payload.status,
            "server_version": server_version
        }
    record = fake_server_db[payload.id]
    return InspectionSyncResponse(
        id=record["id"],
        title=record["title"],
        status="SYNCED",
        server_version=record["server_version"],
        message="Successfully synchronized"
    )