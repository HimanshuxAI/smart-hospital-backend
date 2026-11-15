package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

@SpringBootTest
class JwtServiceTest extends MySqlIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void accessTokenContainsRequiredClaims() {
        User user = persistUser();

        Jwt jwt = jwtDecoder.decode(jwtService.issueAccessToken(user));

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo(UserRole.PATIENT.name());
        assertThat(jwt.getClaimAsString(JwtService.TYPE_CLAIM)).isEqualTo(JwtService.ACCESS_TOKEN_TYPE);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("smart-hospital-backend");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(jwt.getId()).isNotBlank();
    }

    @Test
    void expiredAccessTokenIsRejected() {
        User user = persistUser();
        Instant issuedAt = Instant.now().minusSeconds(180);
        String token = encode(
                user.getId().toString(),
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                issuedAt,
                issuedAt.plusSeconds(60));

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithWrongIssuerIsRejected() {
        User user = persistUser();
        String token = encode(user.getId().toString(), "wrong-issuer", JwtService.ACCESS_TOKEN_TYPE);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithWrongTypeIsRejected() {
        User user = persistUser();
        String token = encode(user.getId().toString(), "smart-hospital-backend", "refresh");

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenForDeactivatedUserIsRejected() {
        User user = persistUser();
        String token = jwtService.issueAccessToken(user);
        user.setActive(false);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private String encode(String subject, String issuer, String type) {
        Instant now = Instant.now();
        return encode(subject, issuer, type, now, now.plusSeconds(900));
    }

    private String encode(
            String subject,
            String issuer,
            String type,
            Instant issuedAt,
            Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .claim(JwtService.ROLE_CLAIM, UserRole.PATIENT.name())
                .claim(JwtService.TYPE_CLAIM, type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private User persistUser() {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "JWT Test Patient",
                "jwt-" + unique + "@example.test",
                "not-used-for-jwt-validation",
                "+1-555-0100"));
    }
}
