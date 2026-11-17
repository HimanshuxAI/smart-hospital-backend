from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


DEFAULT_ARTIFACT_DIR = Path(__file__).resolve().parents[1] / ".artifacts"


@dataclass(frozen=True)
class Settings:
    artifact_dir: Path
    seed: int = 41
    training_row_count: int = 1_200
    model_version: str = "waiting-time-rf-v1"
    feature_schema_version: int = 1
    training_data_provenance: str = "synthetic-demo-data"

    @classmethod
    def from_env(cls) -> "Settings":
        artifact_dir = Path(
            os.environ.get("ML_ARTIFACT_DIR", str(DEFAULT_ARTIFACT_DIR))
        ).expanduser()
        return cls(
            artifact_dir=artifact_dir,
            seed=int(os.environ.get("ML_MODEL_SEED", "41")),
            training_row_count=int(
                os.environ.get("ML_TRAINING_ROW_COUNT", "1200")
            ),
        )

    @property
    def model_path(self) -> Path:
        return self.artifact_dir / "model.joblib"

    @property
    def metadata_path(self) -> Path:
        return self.artifact_dir / "model-metadata.json"
