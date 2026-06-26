package com.flowpilot.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.flowpilot.user.AppUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final long expirationSeconds;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now(clock);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .claim("roles", List.of(user.getRole().name()))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
