package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class RefreshTokenLockingTest extends MySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void distinctIncomingTokensSerializeAcrossOneFamilyLock() throws Exception {
        User user = persistUser();
        String familyId = "family-" + UUID.randomUUID();
        RefreshToken first = persistToken(user, "a".repeat(64), familyId);
        RefreshToken second = persistToken(user, "b".repeat(64), familyId);
        RefreshToken third = persistToken(user, "c".repeat(64), familyId);
        List<Long> expectedIds = List.of(first.getId(), second.getId(), third.getId()).stream()
                .sorted()
                .toList();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch incomingTokensResolved = new CountDownLatch(2);
        CountDownLatch startFamilyLocks = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<LockResult> firstResult = executor.submit(() -> transactionTemplate.execute(status -> {
                RefreshToken incoming = refreshTokenRepository
                        .findByTokenHash(first.getTokenHash())
                        .orElseThrow();
                incomingTokensResolved.countDown();
                await(incomingTokensResolved);
                await(startFamilyLocks);
                return lockFamilyAndRevalidateIncoming(incoming);
            }));

            Future<LockResult> secondResult = executor.submit(() -> transactionTemplate.execute(status -> {
                RefreshToken incoming = refreshTokenRepository
                        .findByTokenHash(third.getTokenHash())
                        .orElseThrow();
                incomingTokensResolved.countDown();
                await(incomingTokensResolved);
                await(startFamilyLocks);
                return lockFamilyAndRevalidateIncoming(incoming);
            }));

            assertThat(incomingTokensResolved.await(5, TimeUnit.SECONDS)).isTrue();
            startFamilyLocks.countDown();

            LockResult firstLock = requireSuccess(firstResult);
            LockResult secondLock = requireSuccess(secondResult);
            assertThat(firstLock.incomingId()).isEqualTo(first.getId());
            assertThat(secondLock.incomingId()).isEqualTo(third.getId());
            assertThat(firstLock.familyIds()).containsExactlyElementsOf(expectedIds);
            assertThat(secondLock.familyIds()).containsExactlyElementsOf(expectedIds);
        } finally {
            startFamilyLocks.countDown();
            executor.shutdownNow();
        }
    }

    private LockResult lockFamilyAndRevalidateIncoming(RefreshToken incoming) {
        List<RefreshToken> lockedFamily = refreshTokenRepository
                .findByFamilyIdForUpdate(incoming.getFamilyId());
        RefreshToken lockedIncoming = lockedFamily.stream()
                .filter(token -> token.getTokenHash().equals(incoming.getTokenHash()))
                .findFirst()
                .orElseThrow();
        assertThat(lockedIncoming.getFamilyId()).isEqualTo(incoming.getFamilyId());
        return new LockResult(
                lockedIncoming.getId(),
                lockedFamily.stream().map(RefreshToken::getId).toList());
    }

    private LockResult requireSuccess(Future<LockResult> result) throws Exception {
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new AssertionError(
                    "both family transactions must commit without deadlock",
                    exception.getCause());
        }
    }

    private record LockResult(Long incomingId, List<Long> familyIds) {
    }

    private User persistUser() {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Synthetic User",
                "patient-" + unique + "@example.test",
                "not-used-in-this-test",
                null));
    }

    private RefreshToken persistToken(User user, String tokenHash, String familyId) {
        return refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                tokenHash,
                familyId,
                Instant.parse("2032-01-01T00:00:00Z")));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test coordination latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for test coordination latch", exception);
        }
    }
}
