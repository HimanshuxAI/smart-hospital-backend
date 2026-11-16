package com.smarthospital.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupJob {

    private final RefreshTokenCleanupService cleanupService;

    public RefreshTokenCleanupJob(RefreshTokenCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "${smart-hospital.security.refresh-token.cleanup-cron}")
    public void cleanupExpiredFamilies() {
        cleanupService.cleanupExpiredFamilies();
    }
}
