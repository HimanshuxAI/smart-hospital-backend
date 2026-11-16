package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class ActiveUserJwtValidatorTest {

    @Test
    void repositoryOutageFailsClosed() {
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        when(userRepository.findById(1L))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        ActiveUserJwtValidator validator = new ActiveUserJwtValidator(userRepository);
        Instant now = Instant.parse("2030-01-15T00:00:00Z");
        Jwt token = Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .issuer("smart-hospital-backend")
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim(JwtService.ROLE_CLAIM, UserRole.PATIENT.name())
                .claim(JwtService.TYPE_CLAIM, JwtService.ACCESS_TOKEN_TYPE)
                .claim(JwtService.AUTH_VERSION_CLAIM, 0L)
                .build();

        OAuth2TokenValidatorResult result = validator.validate(token);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .singleElement()
                .extracting(error -> error.getErrorCode())
                .isEqualTo("invalid_token");
    }
}
