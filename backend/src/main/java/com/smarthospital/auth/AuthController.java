package com.smarthospital.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationRateLimiter rateLimiter;

    public AuthController(
            AuthService authService,
            AuthenticationRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public TokenBundleResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.enforce(
                AuthenticationRateLimiter.Action.REGISTRATION,
                normalizeAccount(request.email()),
                servletRequest.getRemoteAddr());
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenBundleResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.enforce(
                AuthenticationRateLimiter.Action.LOGIN,
                normalizeAccount(request.email()),
                servletRequest.getRemoteAddr());
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenBundleResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.enforce(
                AuthenticationRateLimiter.Action.REFRESH,
                request.refreshToken(),
                servletRequest.getRemoteAddr());
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(Long.valueOf(jwt.getSubject()), request.refreshToken());
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(Long.valueOf(jwt.getSubject()));
    }

    private static String normalizeAccount(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
