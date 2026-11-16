from __future__ import annotations

import json
import math
import os
import pickle
from pathlib import Path
from typing import Any

import joblib
import numpy as np
from sklearn.ensemble import RandomForestRegressor

from app.config import Settings
from app.schemas import PredictionRequest, PredictionResponse
from app.training import TRIAGE_TIER_VALUES, generate_synthetic_history, train_model


FEATURE_NAMES = (
    "queuePosition",
    "peopleWaiting",
    "averageConsultationMinutes",
    "activeEmergencyCount",
    "triageTier",
    "hourOfDay",
    "dayOfWeek",
    "minutesFromScheduledStart",
)


class WaitingTimeModel:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._model: RandomForestRegressor | None = None
        self._metadata: dict[str, Any] | None = None
        self._load_or_train()

    @property
    def metadata(self) -> dict[str, Any]:
        if self._metadata is None:
            raise RuntimeError("model metadata is not initialized")
        return dict(self._metadata)

    def predict(self, request: PredictionRequest) -> PredictionResponse:
        model = self._model
        metadata = self.metadata
        if model is None:
            raise RuntimeError("model is not initialized")

        features = self._transform(request)
        raw_prediction = float(model.predict(features)[0])
        estimated_wait = max(0, int(math.floor(raw_prediction + 0.5)))
        tree_predictions = np.array(
            [float(estimator.predict(features)[0]) for estimator in model.estimators_]
        )
        spread = float(np.std(tree_predictions))
        confidence = round(max(0.0, min(1.0, 1.0 - spread / max(abs(raw_prediction), 1.0))), 4)

        return PredictionResponse(
            estimatedWaitMinutes=estimated_wait,
            confidence=confidence,
            modelVersion=str(metadata["modelVersion"]),
            featureSchemaVersion=int(metadata["featureSchemaVersion"]),
            trainingDataProvenance=str(metadata["trainingDataProvenance"]),
        )

    def _load_or_train(self) -> None:
        if self._load_compatible_artifact():
            return
        self._retrain_and_persist()

    def _load_compatible_artifact(self) -> bool:
        model_path = self.settings.model_path
        metadata_path = self.settings.metadata_path
        if not model_path.is_file() or not metadata_path.is_file():
            return False

        try:
            metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
            if not self._metadata_is_compatible(metadata):
                return False
            model = joblib.load(model_path)
            if not isinstance(model, RandomForestRegressor):
                return False
            if int(model.n_features_in_) != len(FEATURE_NAMES):
                return False
        except (
                OSError,
                ValueError,
                TypeError,
                json.JSONDecodeError,
                EOFError,
                KeyError,
                ImportError,
                AttributeError,
                pickle.UnpicklingError,
        ):
            return False

        self._model = model
        self._metadata = metadata
        return True

    def _metadata_is_compatible(self, metadata: object) -> bool:
        if not isinstance(metadata, dict):
            return False
        expected_values = {
            "modelVersion": self.settings.model_version,
            "featureSchemaVersion": self.settings.feature_schema_version,
            "trainingRowCount": self.settings.training_row_count,
            "seed": self.settings.seed,
            "trainingDataProvenance": "synthetic-demo-data",
            "featureNames": list(FEATURE_NAMES),
            "modelType": "RandomForestRegressor",
        }
        return all(metadata.get(key) == value for key, value in expected_values.items())

    def _retrain_and_persist(self) -> None:
        features, targets = generate_synthetic_history(self.settings)
        model = train_model(features, targets, self.settings.seed)
        metadata: dict[str, Any] = {
            "modelVersion": self.settings.model_version,
            "featureSchemaVersion": self.settings.feature_schema_version,
            "trainingRowCount": self.settings.training_row_count,
            "seed": self.settings.seed,
            "trainingDataProvenance": "synthetic-demo-data",
            "featureNames": list(FEATURE_NAMES),
            "modelType": "RandomForestRegressor",
        }

        self.settings.artifact_dir.mkdir(parents=True, exist_ok=True)
        self._persist_model(model)
        self._persist_metadata(metadata)
        self._model = model
        self._metadata = metadata

    def _persist_model(self, model: RandomForestRegressor) -> None:
        target = self.settings.model_path
        temporary = target.with_suffix(target.suffix + ".tmp")
        joblib.dump(model, temporary)
        os.replace(temporary, target)

    def _persist_metadata(self, metadata: dict[str, Any]) -> None:
        target = self.settings.metadata_path
        temporary = target.with_suffix(target.suffix + ".tmp")
        temporary.write_text(
            json.dumps(metadata, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        os.replace(temporary, target)

    @staticmethod
    def _transform(request: PredictionRequest) -> np.ndarray:
        try:
            triage_value = TRIAGE_TIER_VALUES.index(request.triageTier.upper())
        except ValueError as exception:
            raise ValueError(f"unsupported triage tier: {request.triageTier}") from exception
        return np.array(
            [
                [
                    request.queuePosition,
                    request.peopleWaiting,
                    request.averageConsultationMinutes,
                    request.activeEmergencyCount,
                    triage_value,
                    request.hourOfDay,
                    request.dayOfWeek,
                    request.minutesFromScheduledStart,
                ]
            ],
            dtype=float,
        )
