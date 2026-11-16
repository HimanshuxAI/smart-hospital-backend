package com.smarthospital.department;

import jakarta.validation.constraints.NotNull;

public record DepartmentActivationRequest(@NotNull Boolean active) {
}
