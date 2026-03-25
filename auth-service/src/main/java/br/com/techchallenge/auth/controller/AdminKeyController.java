package br.com.techchallenge.auth.controller;

import br.com.techchallenge.auth.dto.RotateKeyResponse;
import br.com.techchallenge.auth.entity.SigningKey;
import br.com.techchallenge.auth.service.SigningKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/keys")
public class AdminKeyController {

    private final SigningKeyService signingKeyService;

    public AdminKeyController(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @PostMapping("/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public RotateKeyResponse rotate() {
        String retiredKid = signingKeyService.activeSigningKey().getKeyId();
        SigningKey next = signingKeyService.rotateKeys();
        return new RotateKeyResponse(next.getKeyId(), retiredKid);
    }

    @PostMapping("/retire-previous")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirePrevious() {
        signingKeyService.retirePreviousKeys();
    }
}
