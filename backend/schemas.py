from pydantic import BaseModel, Field


class InspectionSyncRequest(BaseModel):
    id: str  # Client-generated stable inspection identifier
    title: str
    status: str
    local_version: int = Field(ge=0)


class InspectionSyncResponse(BaseModel):
    id: str
    title: str
    status: str
    server_version: int
    message: str
