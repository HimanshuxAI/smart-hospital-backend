from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class PredictionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    queuePosition: int = Field(ge=0)
    peopleWaiting: int = Field(ge=0)
    averageConsultationMinutes: float = Field(gt=0)
    activeEmergencyCount: int = Field(ge=0)
    triageTier: Literal["NORMAL", "MEDIUM", "HIGH", "CRITICAL"]
    hourOfDay: int = Field(ge=0, le=23)
    dayOfWeek: int = Field(ge=1, le=7)
    minutesFromScheduledStart: int = Field(ge=0)


class PredictionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    estimatedWaitMinutes: int = Field(ge=0)
    confidence: float = Field(ge=0.0, le=1.0)
    modelVersion: str
    featureSchemaVersion: int = Field(gt=0)
    trainingDataProvenance: str
