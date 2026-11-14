from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(
    title="Smart Hospital ML Service",
    version="1.0.0",
    description="Waiting-time prediction service for Smart Hospital.",
)


class HealthResponse(BaseModel):
    status: str
    service: str


@app.get("/health", tags=["Health"], summary="Service health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="UP", service="smart-hospital-ml")
