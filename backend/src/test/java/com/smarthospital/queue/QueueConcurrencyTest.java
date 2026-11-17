package com.smarthospital.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class QueueConcurrencyTest extends QueueTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentCallsAllowOnlyOneCalledEntryForDoctor() throws Exception {
        TestPatient firstPatient = createPatient();
        TestPatient secondPatient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry first = createQueueEntry(
                firstPatient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        QueueEntry second = createQueueEntry(
                secondPatient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> firstResult = executor.submit(() -> call(
                    doctor.accessToken(), first.getId(), ready, start));
            Future<Integer> secondResult = executor.submit(() -> call(
                    doctor.accessToken(), second.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from queue_entries where doctor_id = ? and status = 'CALLED'",
                    Integer.class,
                    doctor.doctorId()))
                    .isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from queue_entries where doctor_id = ? and status = 'WAITING'",
                    Integer.class,
                    doctor.doctorId()))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentServesAllowOnlyOneEntryInConsultation() throws Exception {
        TestPatient firstPatient = createPatient();
        TestPatient secondPatient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry first = createQueueEntry(
                firstPatient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        QueueEntry second = createQueueEntry(
                secondPatient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        first.call(NOW);
        second.call(NOW);
        queueEntryRepository.saveAndFlush(first);
        queueEntryRepository.saveAndFlush(second);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> firstResult = executor.submit(() -> serve(
                    doctor.accessToken(), first.getId(), ready, start));
            Future<Integer> secondResult = executor.submit(() -> serve(
                    doctor.accessToken(), second.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from queue_entries where doctor_id = ? "
                            + "and status = 'IN_CONSULTATION'",
                    Integer.class,
                    doctor.doctorId()))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private int call(
            String token,
            Long entryId,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        await(start);
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/queue/" + entryId + "/call")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private int serve(
            String token,
            Long entryId,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        await(start);
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/queue/" + entryId + "/serve")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for concurrency latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for concurrency latch", exception);
        }
    }
}
