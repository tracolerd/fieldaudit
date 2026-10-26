from pydantic import BaseModel
from typing import Optional
class InspectionSyncRequest(BaseModel):
    id: str  # Client generated UUID (Idempotency Key)
    title: str
    status: str
    local_version: int
class InspectionSyncResponse(BaseModel):
    id: str
    title: str
    status: str
    server_version: int
    message: str