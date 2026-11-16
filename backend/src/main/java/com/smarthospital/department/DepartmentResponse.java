package com.smarthospital.department;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
