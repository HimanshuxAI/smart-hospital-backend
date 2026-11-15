package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;

import com.smarthospital.common.ApiException;
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
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class RefreshTokenServiceTest extends MySqlIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoSpyBean
    private RefreshTokenRepository spiedRefreshTokenRepository;

    @Test
    void issuedRawTokenIsNeverStored() {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(user);

        assertThat(issued.rawToken()).hasSize(43).doesNotContain("=");
        assertThat(issued.tokenHash()).hasSize(64);
        assertThat(refreshTokenRepository.findByTokenHash(issued.tokenHash())).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash(issued.rawToken())).isEmpty();
        assertThat(refreshTokenRepository.findAll())
                .allSatisfy(token -> assertThat(token.getTokenHash()).isNotEqualTo(issued.rawToken()));
    }

    @Test
    void rotationRevokesOldTokenStoresReplacementAndIssuesSuccessorInSameFamily() {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken original = refreshTokenService.issue(user);

        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(original.rawToken());

        assertThat(rotation.rawToken()).isNotEqualTo(original.rawToken()).hasSize(43);
        assertThat(rotation.familyId()).isEqualTo(original.familyId());
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(original.tokenHash()).orElseThrow();
        RefreshToken successor = refreshTokenRepository.findByTokenHash(rotation.tokenHash()).orElseThrow();
        assertThat(oldToken.getRevokedAt()).isNotNull();
        assertThat(oldToken.getReplacementTokenHash()).isEqualTo(rotation.tokenHash());
        assertThat(successor.getFamilyId()).isEqualTo(original.familyId());
        assertThat(successor.getRevokedAt()).isNull();
        assertThat(successor.getExpiresAt()).isAfter(original.expiresAt());
    }

    @Test
    void rotationUsesNonLockingLookupBeforeLockingTheWholeFamily() {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken original = refreshTokenService.issue(user);
        clearInvocations(spiedRefreshTokenRepository);

        refreshTokenService.rotate(original.rawToken());

        InOrder order = inOrder(spiedRefreshTokenRepository);
        order.verify(spiedRefreshTokenRepository).findByTokenHash(original.tokenHash());
        order.verify(spiedRefreshTokenRepository).findByFamilyIdForUpdate(original.familyId());
    }

    @Test
    void reusingRevokedTokenRevokesEntireFamilyIncludingSuccessor() {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken original = refreshTokenService.issue(user);
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(original.rawToken());

        assertThatThrownBy(() -> refreshTokenService.rotate(original.rawToken()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(401);
                    assertThat(exception.code()).isEqualTo("INVALID_REFRESH_TOKEN");
                });

        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(original.familyId());
        assertThat(family).hasSize(2);
        assertThat(family).allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
        assertThat(refreshTokenRepository.findByTokenHash(rotation.tokenHash()))
                .get()
                .extracting(RefreshToken::getRevokedAt)
                .isNotNull();
    }

    @Test
    void logoutRevokesPresentedTokenAndLeavesNoUsableSuccessor() {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken original = refreshTokenService.issue(user);
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(original.rawToken());

        refreshTokenService.revokeFamily(original.rawToken(), user.getId());

        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(original.familyId());
        assertThat(family).allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
        assertThatThrownBy(() -> refreshTokenService.rotate(rotation.rawToken()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void inactiveUserCannotRefresh() {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(user);
        user.setActive(false);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> refreshTokenService.rotate(issued.rawToken()))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("ACCOUNT_INACTIVE");
                });
    }

    @Test
    void expiredUnknownAndMalformedTokensHaveTheSameStableFailure() {
        User user = persistUser();
        String rawToken = RefreshTokenService.generateRawToken();
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                RefreshTokenService.hash(rawToken),
                UUID.randomUUID().toString(),
                Instant.now().minusSeconds(1)));

        assertInvalidRefresh(() -> refreshTokenService.rotate(rawToken));
        assertInvalidRefresh(() -> refreshTokenService.rotate("not-a-valid-token"));
        assertInvalidRefresh(() -> refreshTokenService.rotate(RefreshTokenService.generateRawToken()));
    }

    @Test
    void concurrentRefreshOfOneTokenDoesNotDeadlockAndLeavesNoUsableSuccessor() throws Exception {
        User user = persistUser();
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(user);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<RefreshTokenService.RotationResult>> futures = IntStream.range(0, 2)
                    .mapToObj(index -> executor.submit(() -> {
                        await(start);
                        try {
                            return refreshTokenService.rotate(issued.rawToken());
                        } catch (ApiException exception) {
                            return null;
                        }
                    }))
                    .toList();

            start.countDown();
            List<RefreshTokenService.RotationResult> results = futures.stream()
                    .map(this::requireResult)
                    .toList();

            assertThat(results).filteredOn(result -> result != null).hasSize(1);
            assertThat(refreshTokenRepository.findByFamilyId(issued.familyId()))
                    .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void assertInvalidRefresh(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(401);
                    assertThat(exception.code()).isEqualTo("INVALID_REFRESH_TOKEN");
                    assertThat(exception.getMessage()).isEqualTo("Refresh token is invalid or expired");
                });
    }

    private RefreshTokenService.RotationResult requireResult(
            Future<RefreshTokenService.RotationResult> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new AssertionError("concurrent refresh must not deadlock", exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for refresh", exception);
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new AssertionError("concurrent refresh timed out", exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for refresh coordination latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for refresh coordination latch", exception);
        }
    }

    private User persistUser() {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Refresh Test Patient",
                "refresh-" + unique + "@example.test",
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
    }
}
