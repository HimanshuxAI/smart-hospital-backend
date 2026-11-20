package com.smarthospital.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.persistence.MySqlIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class ConcurrentUserDeactivationTest extends MySqlIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void concurrentDeactivationOfTheSameAccountIncrementsVersionOnlyOnce() throws Exception {
        User patient = userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Concurrent Deactivation Patient",
                "concurrent-deactivation-" + UUID.randomUUID() + "@example.test",
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        InitialUserLoadBarrier initialLoadBarrier = new InitialUserLoadBarrier(patient.getId());
        registerPostLoadListener(initialLoadBarrier);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<AdminUserResponse> first = executor.submit(
                    () -> userService.setActivation(patient.getId(), false));
            Future<AdminUserResponse> second = executor.submit(
                    () -> userService.setActivation(patient.getId(), false));

            assertThat(first.get(10, TimeUnit.SECONDS).active()).isFalse();
            assertThat(second.get(10, TimeUnit.SECONDS).active()).isFalse();
        } finally {
            executor.shutdownNow();
        }

        User persisted = userRepository.findById(patient.getId()).orElseThrow();
        assertThat(persisted.isActive()).isFalse();
        assertThat(persisted.getAuthVersion()).isEqualTo(1);
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
                throw new AssertionError("timed out coordinating concurrent deactivation reads");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while coordinating concurrent deactivation reads", exception);
        }
    }

    private static final class InitialUserLoadBarrier implements PostLoadEventListener {

        private final Long targetUserId;
        private final CountDownLatch bothInitialReads = new CountDownLatch(2);
        private final Set<Long> participatingThreads = ConcurrentHashMap.newKeySet();

        private InitialUserLoadBarrier(Long targetUserId) {
            this.targetUserId = targetUserId;
        }

        @Override
        public void onPostLoad(PostLoadEvent event) {
            if (!(event.getEntity() instanceof User user)
                    || !targetUserId.equals(user.getId())
                    || !participatingThreads.add(Thread.currentThread().threadId())) {
                return;
            }

            bothInitialReads.countDown();
            await(bothInitialReads);
        }
    }
}
