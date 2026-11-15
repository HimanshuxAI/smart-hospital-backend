package com.smarthospital.user;

public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        boolean active,
        String phone) {
}
