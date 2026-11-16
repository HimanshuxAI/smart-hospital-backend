package com.smarthospital.common;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsFactory {

    private final Clock clock;

    public ProblemDetailsFactory(Clock clock) {
        this.clock = clock;
    }

    public ProblemDetail create(
            HttpStatus status,
            String code,
            String message,
            String path) {
        return create(status, code, message, path, Map.of());
    }

    public ProblemDetail create(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:smart-hospital:problem:" + code.toLowerCase(Locale.ROOT)));
        problem.setInstance(URI.create(path));
        problem.setProperty("code", code);
        problem.setProperty("message", message);
        problem.setProperty("path", path);
        problem.setProperty("timestamp", Instant.now(clock));
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return problem;
    }
}
