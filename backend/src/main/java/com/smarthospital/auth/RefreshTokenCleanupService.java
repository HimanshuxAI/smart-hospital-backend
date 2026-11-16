package com.smarthospital.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    public RefreshTokenCleanupService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Deletes at most one bounded batch of refresh families. Candidate selection is
     * deliberately non-locking; each family is then locked as a complete unit before
     * deletion, preserving the global family-first refresh lock order.
     */
    @Transactional
    public int cleanupExpiredFamilies() {
        Instant cutoff = clock.instant().minus(properties.cleanupRetention());
        List<String> familyIds = refreshTokenRepository.findExpiredFamilyIds(
                cutoff,
                PageRequest.of(0, properties.cleanupBatchSize()));
        int deletedRows = 0;

        for (String familyId : familyIds) {
            List<RefreshToken> family = refreshTokenRepository.findByFamilyIdForUpdate(familyId);
            if (family.isEmpty() || family.stream().anyMatch(token -> !token.getFamilyDeadline().isBefore(cutoff))) {
                continue;
            }
            refreshTokenRepository.deleteAll(family);
            deletedRows += family.size();
        }
        refreshTokenRepository.flush();
        return deletedRows;
    }
}
