from __future__ import annotations

from fastapi import FastAPI, Request
from pydantic import BaseModel

from app.config import Settings
from app.model import WaitingTimeModel
from app.schemas import PredictionRequest, PredictionResponse


class HealthResponse(BaseModel):
    status: str
    service: str


def create_app(settings: Settings | None = None) -> FastAPI:
    application = FastAPI(
        title="Smart Hospital ML Service",
        version="1.0.0",
        description="Waiting-time prediction service for Smart Hospital.",
    )
    application.state.waiting_time_model = WaitingTimeModel(
        settings if settings is not None else Settings.from_env()
    )

    @application.get(
        "/health",
        tags=["Health"],
        summary="Service health",
        response_model=HealthResponse,
    )
    def health() -> HealthResponse:
        return HealthResponse(status="UP", service="smart-hospital-ml")

    @application.post(
        "/predict",
        tags=["Waiting Time"],
        summary="Predict queue waiting time",
        response_model=PredictionResponse,
    )
    def predict(request: PredictionRequest, http_request: Request) -> PredictionResponse:
        model: WaitingTimeModel = http_request.app.state.waiting_time_model
        return model.predict(request)

    return application


app = create_app()
