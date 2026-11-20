package com.smarthospital.auth;

import com.smarthospital.user.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String ROLE_CLAIM = "role";
    public static final String TYPE_CLAIM = "type";
    public static final String AUTH_VERSION_CLAIM = "auth_version";

    private final JwtEncoder jwtEncoder;
    private final TokenProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, TokenProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issueAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(AUTH_VERSION_CLAIM, user.getAuthVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public int accessTokenExpiresInSeconds() {
        return Math.toIntExact(properties.accessTokenExpiration().toSeconds());
    }
}
