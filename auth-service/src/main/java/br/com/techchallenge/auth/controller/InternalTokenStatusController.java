package br.com.techchallenge.auth.controller;

import br.com.techchallenge.auth.config.ApplicationSecurityProperties;
import br.com.techchallenge.auth.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class InternalTokenStatusController {

    private final RefreshTokenService refreshTokenService;
    private final ApplicationSecurityProperties properties;

    public InternalTokenStatusController(RefreshTokenService refreshTokenService, ApplicationSecurityProperties properties) {
        this.refreshTokenService = refreshTokenService;
        this.properties = properties;
    }

    @GetMapping("/internal/tokens/revoked/{jti}")
    public Map<String, Object> revoked(@PathVariable String jti,
                                       @RequestHeader(value = "X-Internal-Secret", required = false) String internalSecret) {
        if (!properties.getInternalSecret().equals(internalSecret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Segredo interno inválido");
        }
        return Map.of("jti", jti, "revoked", refreshTokenService.isRevoked(jti));
    }
}
