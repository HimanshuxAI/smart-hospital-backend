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
    void familyLockCoversEveryTokenInIdOrderAndBlocksUntilCommit() throws Exception {
        User user = persistUser();
        String familyId = "family-" + UUID.randomUUID();
        RefreshToken first = persistToken(user, "a".repeat(64), familyId);
        RefreshToken second = persistToken(user, "b".repeat(64), familyId);
        RefreshToken third = persistToken(user, "c".repeat(64), familyId);
        List<Long> expectedIds = List.of(first.getId(), second.getId(), third.getId()).stream()
                .sorted()
                .toList();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<List<Long>> firstResult = executor.submit(() -> transactionTemplate.execute(status -> {
                List<RefreshToken> locked = refreshTokenRepository.findByFamilyIdForUpdate(familyId);
                firstLocked.countDown();
                await(releaseFirst);
                return locked.stream().map(RefreshToken::getId).toList();
            }));

            Future<List<Long>> secondResult = executor.submit(() -> transactionTemplate.execute(status -> {
                secondStarted.countDown();
                await(firstLocked);
                List<RefreshToken> locked = refreshTokenRepository.findByFamilyIdForUpdate(familyId);
                secondAcquired.countDown();
                return locked.stream().map(RefreshToken::getId).toList();
            }));

            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondAcquired.await(250, TimeUnit.MILLISECONDS))
                    .as("competing family lock must wait for the first transaction")
                    .isFalse();

            releaseFirst.countDown();

            assertThat(firstResult.get(10, TimeUnit.SECONDS)).containsExactlyElementsOf(expectedIds);
            assertThat(secondResult.get(10, TimeUnit.SECONDS)).containsExactlyElementsOf(expectedIds);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
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
