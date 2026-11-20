package com.smarthospital.common;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

    @Test
    void unexpectedFailureLogContainsSanitizedStackWithoutExceptionMessages() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(new ProblemDetailsFactory(
                Clock.fixed(Instant.parse("2030-01-15T00:00:00Z"), ZoneOffset.UTC)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRequestURI("/api/v1/test");
        RuntimeException failure = new IllegalStateException(
                "database password=SensitiveDatabasePassword",
                new IllegalArgumentException("cause token=SensitiveCauseValue"));

        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        ResponseEntity<ProblemDetail> response;
        try {
            response = handler.handleUnexpected(failure, request);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties().get("code")).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getDetail())
                .doesNotContain("SensitiveDatabasePassword", "SensitiveCauseValue");

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        String formattedMessage = event.getFormattedMessage();
        assertThat(formattedMessage)
                .contains("IllegalStateException", "at ")
                .doesNotContain("SensitiveDatabasePassword", "SensitiveCauseValue");
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n")))
                .doesNotContain("SensitiveDatabasePassword", "SensitiveCauseValue");
    }
}
