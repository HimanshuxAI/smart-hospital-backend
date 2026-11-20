package com.smarthospital.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarthospital.common.ApiException;
import com.smarthospital.persistence.MySqlIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AdminActivationLifecycleTest extends MySqlIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void isolateAdministrators() {
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .forEach(user -> {
                    user.setActive(false);
                    userRepository.saveAndFlush(user);
                });
    }

    @Test
    void soleActiveAdministratorCannotBeDeactivated() {
        User soleAdmin = persistAdmin();

        assertThatThrownBy(() -> userService.setActivation(soleAdmin.getId(), false))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(409);
                    assertThat(exception.code()).isEqualTo("LAST_ACTIVE_ADMIN");
                });

        assertThat(userRepository.findById(soleAdmin.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void selfDeactivationIsAllowedWhenAnotherActiveAdministratorRemains() {
        User target = persistAdmin();
        persistAdmin();

        AdminUserResponse response = userService.setActivation(target.getId(), false);

        assertThat(response.active()).isFalse();
    }

    @Test
    void concurrentDeactivationsCannotRemoveTheLastActiveAdministrator() throws Exception {
        User first = persistAdmin();
        User second = persistAdmin();
        registerPostLoadListener(new InitialAdminLoadBarrier(Set.of(first.getId(), second.getId())));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<DeactivationOutcome> firstOutcome = executor.submit(() -> deactivateAfter(start, first.getId()));
            Future<DeactivationOutcome> secondOutcome = executor.submit(() -> deactivateAfter(start, second.getId()));

            start.countDown();
            List<DeactivationOutcome> outcomes = List.of(
                    requireOutcome(firstOutcome),
                    requireOutcome(secondOutcome));

            assertThat(outcomes).filteredOn(DeactivationOutcome::succeeded).hasSize(1);
            assertThat(outcomes)
                    .filteredOn(outcome -> !outcome.succeeded())
                    .singleElement()
                    .extracting(DeactivationOutcome::code)
                    .isEqualTo("LAST_ACTIVE_ADMIN");
            assertThat(userRepository.findAll().stream()
                    .filter(user -> user.getRole() == UserRole.ADMIN)
                    .filter(User::isActive))
                    .hasSize(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private DeactivationOutcome deactivateAfter(CountDownLatch start, Long userId) {
        await(start);
        try {
            userService.setActivation(userId, false);
            return new DeactivationOutcome(true, null);
        } catch (ApiException exception) {
            return new DeactivationOutcome(false, exception.code());
        }
    }

    private DeactivationOutcome requireOutcome(Future<DeactivationOutcome> future) throws Exception {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new AssertionError("concurrent admin deactivation failed", exception.getCause());
        }
    }

    private User persistAdmin() {
        return userRepository.saveAndFlush(new User(
                UserRole.ADMIN,
                "Concurrency Admin",
                "admin-" + UUID.randomUUID() + "@example.test",
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
    }

    private void registerPostLoadListener(PostLoadEventListener listener) {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_LOAD, listener);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for admin deactivation coordination latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for admin deactivation coordination latch", exception);
        }
    }

    private record DeactivationOutcome(boolean succeeded, String code) {
    }

    private static final class InitialAdminLoadBarrier implements PostLoadEventListener {

        private final Set<Long> targetAdminIds;
        private final CountDownLatch bothInitialReads = new CountDownLatch(2);
        private final Set<Long> participatingThreads = ConcurrentHashMap.newKeySet();

        private InitialAdminLoadBarrier(Set<Long> targetAdminIds) {
            this.targetAdminIds = targetAdminIds;
        }

        @Override
        public void onPostLoad(PostLoadEvent event) {
            if (!(event.getEntity() instanceof User user)
                    || !targetAdminIds.contains(user.getId())
                    || !participatingThreads.add(Thread.currentThread().threadId())) {
                return;
            }

            bothInitialReads.countDown();
            await(bothInitialReads);
        }
    }
}
