package com.smarthospital.auth;

import com.smarthospital.common.ApiException;
import com.smarthospital.user.User;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final Pattern URL_SAFE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final EntityManager entityManager;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenProperties properties,
            Clock clock,
            EntityManager entityManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        String familyId = UUID.randomUUID().toString();
        Instant now = clock.instant();
        Instant familyDeadline = now.plus(properties.familyLifetime());
        Instant expiresAt = earlier(now.plus(properties.expiration()), familyDeadline);
        RefreshToken token = refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                tokenHash,
                familyId,
                expiresAt,
                familyDeadline,
                user.getAuthVersion()));
        return new IssuedRefreshToken(rawToken, token.getTokenHash(), token.getFamilyId(), expiresAt);
    }

    /**
     * Rotates a refresh token using the repository's family-first lock contract:
     * a non-locking identity lookup resolves the immutable family id, then the
     * complete family is locked before the incoming token is revalidated.
     *
     * <p>{@code ApiException} does not roll this transaction back. This is required
     * for reuse detection: revoking the successor and rejecting the request must
     * commit as one atomic family revocation.</p>
     */
    @Transactional(noRollbackFor = ApiException.class)
    public RotationResult rotate(String rawToken) {
        validateRawToken(rawToken);
        String tokenHash = hash(rawToken);

        RefreshToken identity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenService::invalidRefreshToken);
        String familyId = identity.getFamilyId();
        entityManager.clear();
        List<RefreshToken> lockedFamily = refreshTokenRepository.findByFamilyIdForUpdate(familyId);
        RefreshToken incoming = locateLockedToken(lockedFamily, tokenHash);

        if (incoming.getRevokedAt() != null) {
            revokeFamilyLocked(lockedFamily, clock.instant());
            refreshTokenRepository.flush();
            throw invalidRefreshToken();
        }
        Instant now = clock.instant();
        if (!incoming.getExpiresAt().isAfter(now)
                || !incoming.getFamilyDeadline().isAfter(now)
                || incoming.getExpiresAt().isAfter(incoming.getFamilyDeadline())) {
            revokeFamilyLocked(lockedFamily, now);
            refreshTokenRepository.flush();
            throw invalidRefreshToken();
        }
        if (incoming.getAuthVersion() != incoming.getUser().getAuthVersion()) {
            revokeFamilyLocked(lockedFamily, now);
            refreshTokenRepository.flush();
            throw invalidRefreshToken();
        }
        if (!incoming.getUser().isActive()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_INACTIVE",
                    "Account is inactive");
        }

        String replacementRawToken = generateRawToken();
        String replacementHash = hash(replacementRawToken);
        Instant replacementExpiresAt = earlier(
                now.plus(properties.expiration()),
                incoming.getFamilyDeadline());
        incoming.replaceWith(replacementHash, now);
        refreshTokenRepository.save(incoming);
        RefreshToken replacement = refreshTokenRepository.save(new RefreshToken(
                incoming.getUser(),
                replacementHash,
                incoming.getFamilyId(),
                replacementExpiresAt,
                incoming.getFamilyDeadline(),
                incoming.getAuthVersion()));
        refreshTokenRepository.flush();

        return new RotationResult(
                incoming.getUser(),
                replacementRawToken,
                replacement.getTokenHash(),
                replacement.getFamilyId(),
                replacementExpiresAt);
    }

    @Transactional
    public void revokeFamily(String rawToken, Long userId) {
        validateRawToken(rawToken);
        String tokenHash = hash(rawToken);
        RefreshToken identity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenService::invalidRefreshToken);
        String familyId = identity.getFamilyId();
        entityManager.clear();
        List<RefreshToken> lockedFamily = refreshTokenRepository.findByFamilyIdForUpdate(familyId);
        RefreshToken presented = locateLockedToken(lockedFamily, tokenHash);
        if (!presented.getUser().getId().equals(userId)) {
            throw invalidRefreshToken();
        }
        revokeFamilyLocked(lockedFamily, clock.instant());
        refreshTokenRepository.flush();
    }

    private static Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private static RefreshToken locateLockedToken(List<RefreshToken> lockedFamily, String tokenHash) {
        return lockedFamily.stream()
                .filter(token -> token.getTokenHash().equals(tokenHash))
                .findFirst()
                .orElseThrow(RefreshTokenService::invalidRefreshToken);
    }

    private static void revokeFamilyLocked(List<RefreshToken> lockedFamily, Instant revokedAt) {
        for (RefreshToken token : lockedFamily) {
            if (token.getRevokedAt() == null) {
                token.revoke(revokedAt);
            }
        }
    }

    static String generateRawToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void validateRawToken(String rawToken) {
        if (rawToken == null || !URL_SAFE_TOKEN.matcher(rawToken).matches()) {
            throw invalidRefreshToken();
        }
        try {
            if (Base64.getUrlDecoder().decode(rawToken).length != TOKEN_BYTES) {
                throw invalidRefreshToken();
            }
        } catch (IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "Refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(
            String rawToken,
            String tokenHash,
            String familyId,
            Instant expiresAt) {

        @Override
        public String toString() {
            return "IssuedRefreshToken[rawToken=REDACTED, tokenHash=REDACTED, familyId="
                    + familyId + ", expiresAt=" + expiresAt + "]";
        }
    }

    public record RotationResult(
            User user,
            String rawToken,
            String tokenHash,
            String familyId,
            Instant expiresAt) {

        @Override
        public String toString() {
            return "RotationResult[rawToken=REDACTED, tokenHash=REDACTED, familyId="
                    + familyId + ", expiresAt=" + expiresAt + "]";
        }
    }
}
