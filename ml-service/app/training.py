from __future__ import annotations

import numpy as np
from sklearn.ensemble import RandomForestRegressor

from app.config import Settings


MAX_BOUNDED_NOISE_MINUTES = 4.0
TRIAGE_TIER_VALUES = ("NORMAL", "MEDIUM", "HIGH", "CRITICAL")


def calculate_target(features: np.ndarray, noise: float) -> float:
    """Calculate a reproducible synthetic wait target with bounded noise."""
    values = np.asarray(features, dtype=float)
    queue_ahead = max(values[0] - 1.0, 0.0)
    people_waiting = max(values[1], 0.0)
    consultation_minutes = max(values[2], 0.0)
    emergency_count = max(values[3], 0.0)
    triage_tier = int(values[4])
    hour_of_day = int(values[5])
    minutes_from_start = max(values[7], 0.0)

    peak_load = 6.0 if hour_of_day in (8, 9, 11, 12, 13, 14, 15, 16) else 0.0
    triage_load = max(triage_tier - 1, 0) * 2.0
    target = (
        (queue_ahead + people_waiting) * consultation_minutes
        + emergency_count * 5.0
        + triage_load
        + peak_load
        + min(minutes_from_start / 30.0, 8.0)
    )
    bounded_noise = float(np.clip(noise, -MAX_BOUNDED_NOISE_MINUTES, MAX_BOUNDED_NOISE_MINUTES))
    return max(target + bounded_noise, 0.0)


def generate_synthetic_history(settings: Settings) -> tuple[np.ndarray, np.ndarray]:
    random = np.random.default_rng(settings.seed)
    row_count = settings.training_row_count

    queue_position = random.integers(0, 31, size=row_count)
    people_waiting = random.integers(0, 31, size=row_count)
    average_consultation = random.choice(
        np.array([10.0, 15.0, 20.0, 25.0]), size=row_count
    )
    emergency_count = random.integers(0, 9, size=row_count)
    triage_tier = random.integers(0, len(TRIAGE_TIER_VALUES), size=row_count)
    hour_of_day = random.integers(0, 24, size=row_count)
    day_of_week = random.integers(1, 8, size=row_count)
    minutes_from_start = random.integers(0, 241, size=row_count)

    features = np.column_stack(
        (
            queue_position,
            people_waiting,
            average_consultation,
            emergency_count,
            triage_tier,
            hour_of_day,
            day_of_week,
            minutes_from_start,
        )
    )
    noise = random.uniform(
        -MAX_BOUNDED_NOISE_MINUTES,
        MAX_BOUNDED_NOISE_MINUTES,
        size=row_count,
    )
    targets = np.array(
        [calculate_target(row, row_noise) for row, row_noise in zip(features, noise)],
        dtype=float,
    )
    return features, targets


def train_model(
    features: np.ndarray,
    targets: np.ndarray,
    seed: int,
) -> RandomForestRegressor:
    model = RandomForestRegressor(
        n_estimators=120,
        max_depth=12,
        min_samples_leaf=2,
        random_state=seed,
        n_jobs=1,
    )
    model.fit(features, targets)
    return model
