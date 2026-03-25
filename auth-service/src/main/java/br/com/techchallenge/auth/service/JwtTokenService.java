package br.com.techchallenge.auth.service;

import br.com.techchallenge.auth.config.ApplicationSecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SigningKeyService signingKeyService;
    private final ApplicationSecurityProperties properties;

    public JwtTokenService(JwtEncoder jwtEncoder, SigningKeyService signingKeyService, ApplicationSecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.signingKeyService = signingKeyService;
        this.properties = properties;
    }

    public IssuedToken issue(Authentication authentication) {
        return issue(authentication.getName(), authentication.getAuthorities());
    }

    public IssuedToken issue(String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.getJwt().getTtl()))
                .subject(username)
                .id(jti)
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKeyService.activeSigningKey().getKeyId())
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(accessToken, jti, now.plus(properties.getJwt().getTtl()));
    }

    public record IssuedToken(String accessToken, String jti, Instant expiresAt) {
    }
}
