import json
from pathlib import Path

import numpy as np
import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.config import Settings
from app.main import create_app
from app.model import WaitingTimeModel
from app.schemas import PredictionRequest, PredictionResponse
from app.training import calculate_target, generate_synthetic_history


REQUEST_FIELDS = {
    "queuePosition",
    "peopleWaiting",
    "averageConsultationMinutes",
    "activeEmergencyCount",
    "triageTier",
    "hourOfDay",
    "dayOfWeek",
    "minutesFromScheduledStart",
}

RESPONSE_FIELDS = {
    "estimatedWaitMinutes",
    "confidence",
    "modelVersion",
    "featureSchemaVersion",
    "trainingDataProvenance",
}


def request_payload(**overrides: object) -> dict[str, object]:
    payload: dict[str, object] = {
        "queuePosition": 3,
        "peopleWaiting": 5,
        "averageConsultationMinutes": 15.0,
        "activeEmergencyCount": 1,
        "triageTier": "MEDIUM",
        "hourOfDay": 10,
        "dayOfWeek": 2,
        "minutesFromScheduledStart": 20,
    }
    payload.update(overrides)
    return payload


def test_request_and_response_schemas_expose_exact_fields() -> None:
    assert set(PredictionRequest.model_fields) == REQUEST_FIELDS
    assert set(PredictionResponse.model_fields) == RESPONSE_FIELDS


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("queuePosition", -1),
        ("peopleWaiting", -1),
        ("averageConsultationMinutes", 0.0),
        ("activeEmergencyCount", -1),
        ("hourOfDay", 24),
        ("dayOfWeek", 0),
        ("minutesFromScheduledStart", -1),
    ],
)
def test_request_rejects_negative_or_out_of_range_values(field: str, value: object) -> None:
    with pytest.raises(ValidationError):
        PredictionRequest.model_validate(request_payload(**{field: value}))


def test_request_rejects_unsupported_triage_tier() -> None:
    with pytest.raises(ValidationError):
        PredictionRequest.model_validate(request_payload(triageTier="UNKNOWN"))


def test_prediction_is_nonnegative_and_deterministic(tmp_path: Path) -> None:
    settings = Settings(artifact_dir=tmp_path, seed=17, training_row_count=400)
    model = WaitingTimeModel(settings)
    request = PredictionRequest.model_validate(request_payload())

    first = model.predict(request)
    second = model.predict(request)

    assert first == second
    assert first.estimatedWaitMinutes >= 0
    assert 0.0 <= first.confidence <= 1.0


def test_model_metadata_and_artifacts_are_reproducible(tmp_path: Path) -> None:
    settings = Settings(artifact_dir=tmp_path, seed=23, training_row_count=350)
    model = WaitingTimeModel(settings)

    metadata_path = tmp_path / "model-metadata.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))

    assert metadata["modelVersion"] == settings.model_version
    assert metadata["featureSchemaVersion"] == settings.feature_schema_version
    assert metadata["trainingRowCount"] == settings.training_row_count
    assert metadata["seed"] == settings.seed
    assert metadata["trainingDataProvenance"] == "synthetic-demo-data"
    assert (tmp_path / "model.joblib").is_file()
    assert model.metadata["trainingDataProvenance"] == "synthetic-demo-data"


def test_incompatible_artifact_is_regenerated(tmp_path: Path) -> None:
    settings = Settings(artifact_dir=tmp_path, seed=29, training_row_count=300)
    WaitingTimeModel(settings)
    metadata_path = tmp_path / "model-metadata.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    metadata["featureSchemaVersion"] = settings.feature_schema_version + 1
    metadata_path.write_text(json.dumps(metadata), encoding="utf-8")

    regenerated = WaitingTimeModel(settings)

    regenerated_metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    assert regenerated_metadata["featureSchemaVersion"] == settings.feature_schema_version
    assert regenerated.metadata == regenerated_metadata


def test_corrupt_model_artifact_is_regenerated(tmp_path: Path) -> None:
    settings = Settings(artifact_dir=tmp_path, seed=43, training_row_count=300)
    WaitingTimeModel(settings)
    (tmp_path / "model.joblib").write_bytes(b"not-a-joblib-artifact")

    regenerated = WaitingTimeModel(settings)

    assert regenerated.metadata["featureSchemaVersion"] == settings.feature_schema_version
    assert regenerated.predict(
        PredictionRequest.model_validate(request_payload())
    ).estimatedWaitMinutes >= 0


def test_synthetic_target_increases_with_queue_and_emergency_load() -> None:
    base = np.array([1, 2, 15.0, 0, 0, 10, 2, 10], dtype=float)
    larger_queue = base.copy()
    larger_queue[0] = 10
    larger_queue[1] = 12
    more_emergencies = base.copy()
    more_emergencies[3] = 4

    assert calculate_target(larger_queue, 0.0) > calculate_target(base, 0.0)
    assert calculate_target(more_emergencies, 0.0) > calculate_target(base, 0.0)


def test_synthetic_history_uses_fixed_seed_and_bounded_noise(tmp_path: Path) -> None:
    settings = Settings(artifact_dir=tmp_path, seed=31, training_row_count=500)

    first_features, first_targets = generate_synthetic_history(settings)
    second_features, second_targets = generate_synthetic_history(settings)

    assert first_features.shape == (500, 8)
    assert first_targets.shape == (500,)
    assert np.array_equal(first_features, second_features)
    assert np.array_equal(first_targets, second_targets)
    assert np.all(first_targets >= 0)


def test_predict_api_exposes_exact_contract(tmp_path: Path) -> None:
    app = create_app(Settings(artifact_dir=tmp_path, seed=37, training_row_count=300))
    client = TestClient(app)

    response = client.post("/predict", json=request_payload())

    assert response.status_code == 200
    assert set(response.json()) == RESPONSE_FIELDS
    assert response.json()["estimatedWaitMinutes"] >= 0
    assert response.json()["trainingDataProvenance"] == "synthetic-demo-data"


def test_predict_api_rejects_negative_values(tmp_path: Path) -> None:
    app = create_app(Settings(artifact_dir=tmp_path, seed=41, training_row_count=300))
    client = TestClient(app)

    response = client.post("/predict", json=request_payload(peopleWaiting=-1))

    assert response.status_code == 422
