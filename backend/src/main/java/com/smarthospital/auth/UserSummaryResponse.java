package com.smarthospital.auth;

import com.smarthospital.user.UserRole;

public record UserSummaryResponse(
        Long id,
        String email,
        UserRole role) {

    @Override
    public String toString() {
        return "UserSummaryResponse[id=" + id + ", role=" + role + ", details=REDACTED]";
    }
}
