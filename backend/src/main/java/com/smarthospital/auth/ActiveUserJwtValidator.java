package com.smarthospital.auth;

import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_CLAIMS = new OAuth2Error(
            "invalid_token",
            "Token claims are invalid",
            null);
    private static final OAuth2Error INACTIVE_USER = new OAuth2Error(
            "invalid_token",
            "Token subject is not an active user",
            null);
    private static final OAuth2Error AUTH_VERSION_MISMATCH = new OAuth2Error(
            "invalid_token",
            "Token authentication version does not match",
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
        Long userId = parseUserId(token.getSubject());
        if (userId == null || !hasValidRole(token) || !hasValidAuthVersion(token)) {
            return OAuth2TokenValidatorResult.failure(INVALID_CLAIMS);
        }
        long authVersion = parseAuthVersion(token.getClaims().get(JwtService.AUTH_VERSION_CLAIM));

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                return OAuth2TokenValidatorResult.failure(INACTIVE_USER);
            }
            if (user.getAuthVersion() != authVersion) {
                return OAuth2TokenValidatorResult.failure(AUTH_VERSION_MISMATCH);
            }
            return OAuth2TokenValidatorResult.success();
        } catch (DataAccessException exception) {
            return OAuth2TokenValidatorResult.failure(VALIDATION_UNAVAILABLE);
        }
    }

    private static Long parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            long userId = Long.parseLong(subject);
            return userId > 0 ? userId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean hasValidRole(Jwt token) {
        Object roleClaim = token.getClaims().get(JwtService.ROLE_CLAIM);
        if (!(roleClaim instanceof String role)) {
            return false;
        }
        try {
            UserRole.valueOf(role);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean hasValidAuthVersion(Jwt token) {
        Long version = parseAuthVersion(token.getClaims().get(JwtService.AUTH_VERSION_CLAIM));
        return version != null && version >= 0;
    }

    private static Long parseAuthVersion(Object claim) {
        try {
            if (claim instanceof Byte || claim instanceof Short
                    || claim instanceof Integer || claim instanceof Long) {
                return ((Number) claim).longValue();
            }
            if (claim instanceof BigInteger integer) {
                return integer.longValueExact();
            }
            if (claim instanceof BigDecimal decimal) {
                return decimal.longValueExact();
            }
        } catch (ArithmeticException exception) {
            return null;
        }
        return null;
    }
}
