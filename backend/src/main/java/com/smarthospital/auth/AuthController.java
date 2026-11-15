package com.smarthospital.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(tags = "Public", summary = "Register a patient")
    public TokenBundleResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(tags = "Public", summary = "Authenticate a user")
    public TokenBundleResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(tags = "Public", summary = "Refresh an access token")
    public TokenBundleResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(tags = "Authenticated", summary = "Revoke the presented token family")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(Long.valueOf(jwt.getSubject()), request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(tags = "Authenticated", summary = "Get the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(Long.valueOf(jwt.getSubject()));
    }
}
