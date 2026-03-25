package br.com.techchallenge.auth.controller;

import br.com.techchallenge.auth.service.SigningKeyService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwkSetController {

    private final SigningKeyService signingKeyService;

    public JwkSetController(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return new JWKSet(signingKeyService.publicJwks().stream().map(JWK.class::cast).toList()).toJSONObject();
    }
}
