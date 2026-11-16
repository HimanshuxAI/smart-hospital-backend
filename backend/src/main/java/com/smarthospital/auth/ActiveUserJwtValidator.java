package com.smarthospital.auth;

import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_USER = new OAuth2Error(
            "invalid_token",
            "Token subject is not an active user",
            null);
    private static final OAuth2Error VALIDATION_UNAVAILABLE = new OAuth2Error(
            "invalid_token",
            "Token subject validation failed",
            null);

    private final UserRepository userRepository;

    public ActiveUserJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Long userId;
        try {
            userId = Long.valueOf(token.getSubject());
        } catch (NumberFormatException exception) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_USER);
        }

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.isActive()) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(INACTIVE_USER);
        } catch (DataAccessException exception) {
            return OAuth2TokenValidatorResult.failure(VALIDATION_UNAVAILABLE);
        }
    }
}
