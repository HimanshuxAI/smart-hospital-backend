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
        Object authenticationVersion = jwt.getClaim("auth_version");
        assertThat(authenticationVersion).isInstanceOf(Number.class);
        assertThat(((Number) authenticationVersion).longValue()).isZero();
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

    @Test
    void tokenWithMissingAuthenticationVersionIsRejected() {
        User user = persistUser();

        String token = encodeClaims(
                user.getId().toString(),
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                UserRole.PATIENT.name(),
                null,
                false);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithMalformedAuthenticationVersionIsRejected() {
        User user = persistUser();

        String token = encodeClaims(
                user.getId().toString(),
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                UserRole.PATIENT.name(),
                "not-a-number",
                true);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithMismatchedAuthenticationVersionIsRejected() {
        User user = persistUser();

        String token = encodeClaims(
                user.getId().toString(),
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                UserRole.PATIENT.name(),
                1L,
                true);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithMissingRoleIsRejected() {
        User user = persistUser();

        String token = encodeClaims(
                user.getId().toString(),
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                null,
                0L,
                true);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithInvalidRoleIsRejected() {
        User user = persistUser();

        String token = encodeClaims(
                user.getId().toString(),
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                "SUPER_ADMIN",
                0L,
                true);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithMalformedSubjectIsRejected() {
        User user = persistUser();

        String token = encodeClaims(
                "not-a-user-id",
                "smart-hospital-backend",
                JwtService.ACCESS_TOKEN_TYPE,
                UserRole.PATIENT.name(),
                0L,
                true);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithWrongAlgorithmIsRejected() {
        User user = persistUser();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("smart-hospital-backend")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim(JwtService.ROLE_CLAIM, UserRole.PATIENT.name())
                .claim(JwtService.TYPE_CLAIM, JwtService.ACCESS_TOKEN_TYPE)
                .claim("auth_version", 0L)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS384).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

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
        return encodeClaims(
                subject,
                issuer,
                type,
                UserRole.PATIENT.name(),
                0L,
                true,
                issuedAt,
                expiresAt);
    }

    private String encodeClaims(
            String subject,
            String issuer,
            String type,
            String role,
            Object authenticationVersion,
            boolean includeAuthenticationVersion) {
        Instant now = Instant.now();
        return encodeClaims(
                subject,
                issuer,
                type,
                role,
                authenticationVersion,
                includeAuthenticationVersion,
                now,
                now.plusSeconds(900));
    }

    private String encodeClaims(
            String subject,
            String issuer,
            String type,
            String role,
            Object authenticationVersion,
            boolean includeAuthenticationVersion,
            Instant issuedAt,
            Instant expiresAt) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .claim(JwtService.TYPE_CLAIM, type);
        if (role != null) {
            builder.claim(JwtService.ROLE_CLAIM, role);
        }
        if (includeAuthenticationVersion) {
            builder.claim("auth_version", authenticationVersion);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
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
