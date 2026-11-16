package com.smarthospital.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;

    public SecurityProblemHandler(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper) {
        this.problemDetailsFactory = problemDetailsFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        response.setHeader("WWW-Authenticate", "Bearer");
        writeProblem(
                response,
                request,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        writeProblem(
                response,
                request,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied");
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message) throws IOException {
        ProblemDetail problem = problemDetailsFactory.create(
                status,
                code,
                message,
                request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
