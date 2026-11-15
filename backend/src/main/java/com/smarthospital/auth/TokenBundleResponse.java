package com.smarthospital.auth;

public record TokenBundleResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        UserSummaryResponse user) {

    @Override
    public String toString() {
        return "TokenBundleResponse[accessToken=REDACTED, refreshToken=REDACTED, tokenType="
                + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
