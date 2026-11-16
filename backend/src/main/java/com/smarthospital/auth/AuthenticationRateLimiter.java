package com.smarthospital.auth;

import com.smarthospital.common.RateLimitExceededException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Bounded in-memory fixed-window limiter for unauthenticated authentication
 * endpoints. Identity and token values are hashed before entering the state map,
 * and each action tracks its identity/token and remote-address dimensions
 * independently.
 */
@Component
public class AuthenticationRateLimiter {

    public enum Action {
        LOGIN,
        REGISTRATION,
        REFRESH
    }

    private enum Dimension {
        IDENTITY,
        IP
    }

    private final AuthenticationRateLimitProperties properties;
    private final Clock clock;
    private final Map<WindowKey, Window> windows = new HashMap<>();

    public AuthenticationRateLimiter(
            AuthenticationRateLimitProperties properties,
            Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized void enforce(Action action, String identity, String remoteAddress) {
        Instant now = clock.instant();
        long windowMillis = properties.window().toMillis();
        long windowNumber = Math.floorDiv(now.toEpochMilli(), windowMillis);
        Instant windowStart = Instant.ofEpochMilli(Math.multiplyExact(windowNumber, windowMillis));
        Instant windowEnd = windowStart.plusMillis(windowMillis);

        pruneExpired(now);

        WindowKey identityKey = new WindowKey(action, Dimension.IDENTITY, hash(identity));
        WindowKey ipKey = new WindowKey(action, Dimension.IP, hash(remoteAddress));
        long newEntries = List.of(identityKey, ipKey).stream()
                .distinct()
                .filter(key -> !windows.containsKey(key))
                .count();

        if (windows.size() + newEntries > properties.maxTrackedEntries()) {
            throw rateLimited(secondsUntil(now, earliestExpiry(windowEnd)));
        }

        Window identityWindow = increment(identityKey, windowEnd);
        Window ipWindow = increment(ipKey, windowEnd);
        int identityLimit = identityLimit(action);
        int ipLimit = ipLimit(action);

        if (identityWindow.count > identityLimit || ipWindow.count > ipLimit) {
            throw rateLimited(secondsUntil(now, windowEnd));
        }
    }

    private Window increment(WindowKey key, Instant windowEnd) {
        Window window = windows.computeIfAbsent(key, ignored -> new Window(windowEnd));
        window.count++;
        window.expiresAt = windowEnd;
        return window;
    }

    private void pruneExpired(Instant now) {
        windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
    }

    private Instant earliestExpiry(Instant fallback) {
        return windows.values().stream()
                .map(window -> window.expiresAt)
                .min(Instant::compareTo)
                .orElse(fallback);
    }

    private int identityLimit(Action action) {
        return switch (action) {
            case LOGIN -> properties.login().accountLimit();
            case REGISTRATION -> properties.registration().accountLimit();
            case REFRESH -> properties.refresh().tokenLimit();
        };
    }

    private int ipLimit(Action action) {
        return switch (action) {
            case LOGIN -> properties.login().ipLimit();
            case REGISTRATION -> properties.registration().ipLimit();
            case REFRESH -> properties.refresh().ipLimit();
        };
    }

    private static long secondsUntil(Instant now, Instant end) {
        long remainingMillis = Math.max(1, Duration.between(now, end).toMillis());
        return Math.max(1, (remainingMillis + 999) / 1000);
    }

    private static RateLimitExceededException rateLimited(long retryAfterSeconds) {
        return new RateLimitExceededException(retryAfterSeconds);
    }

    private static String hash(String value) {
        String safeValue = value == null ? "" : value;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(safeValue.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record WindowKey(Action action, Dimension dimension, String digest) {
    }

    private static final class Window {

        private long count;
        private Instant expiresAt;

        private Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
