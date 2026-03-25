package br.com.techchallenge.gateway.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class GatewayJwtRelayService {

    public String bearerToken(Authentication authentication) {
        if (authentication.getCredentials() instanceof Jwt jwt) {
            return "Bearer " + jwt.getTokenValue();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return "Bearer " + jwt.getTokenValue();
        }
        throw new IllegalStateException("Gateway nao conseguiu propagar o JWT autenticado");
    }
}
