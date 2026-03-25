package br.com.techchallenge.auth.service;

import br.com.techchallenge.auth.config.ApplicationSecurityProperties;
import br.com.techchallenge.auth.entity.SigningKey;
import br.com.techchallenge.auth.repository.SigningKeyRepository;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class SigningKeyService {

    private final SigningKeyRepository signingKeyRepository;
    private final ApplicationSecurityProperties properties;

    public SigningKeyService(SigningKeyRepository signingKeyRepository, ApplicationSecurityProperties properties) {
        this.signingKeyRepository = signingKeyRepository;
        this.properties = properties;
    }

    @Transactional
    public void ensureSigningKey() {
        if (signingKeyRepository.findByActiveTrue().isPresent()) {
            return;
        }
        if (properties.getJwt().getPublicKey() != null
                && !properties.getJwt().getPublicKey().isBlank()
                && properties.getJwt().getPrivateKey() != null
                && !properties.getJwt().getPrivateKey().isBlank()) {
            SigningKey key = new SigningKey();
            key.setKeyId(properties.getJwt().getKeyId());
            key.setPublicKey(properties.getJwt().getPublicKey());
            key.setPrivateKey(properties.getJwt().getPrivateKey());
            key.setActive(true);
            key.setRetired(false);
            key.setCreatedAt(Instant.now());
            signingKeyRepository.save(key);
            return;
        }
        rotateKeys();
    }

    public RSAKey activeRsaKey() {
        SigningKey key = activeSigningKey();
        try {
            return new RSAKey.Builder(activePublicKey())
                    .privateKey(parsePrivateKey(key.getPrivateKey()))
                    .keyID(key.getKeyId())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel montar a chave RSA ativa", ex);
        }
    }

    public RSAPublicKey activePublicKey() {
        return parsePublicKey(activeSigningKey().getPublicKey());
    }

    public SigningKey activeSigningKey() {
        return signingKeyRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Nao existe chave ativa configurada"));
    }

    public List<RSAKey> publicJwks() {
        return signingKeyRepository.findByRetiredFalse().stream()
                .map(key -> {
                    try {
                        return new RSAKey.Builder(parsePublicKey(key.getPublicKey()))
                                .keyID(key.getKeyId())
                                .build();
                    } catch (Exception ex) {
                        throw new IllegalStateException("Nao foi possivel montar a chave publica", ex);
                    }
                })
                .toList();
    }

    @Transactional
    public SigningKey rotateKeys() {
        SigningKey previousActive = signingKeyRepository.findByActiveTrue().orElse(null);
        KeyPair keyPair = generateKeyPair();
        SigningKey next = new SigningKey();
        next.setKeyId("kid-" + UUID.randomUUID());
        next.setPublicKey(encode(keyPair.getPublic().getEncoded()));
        next.setPrivateKey(encode(keyPair.getPrivate().getEncoded()));
        next.setActive(true);
        next.setRetired(false);
        next.setCreatedAt(Instant.now());
        signingKeyRepository.save(next);

        if (previousActive != null) {
            previousActive.setActive(false);
            previousActive.setRotatedAt(Instant.now());
            signingKeyRepository.save(previousActive);
        }

        return next;
    }

    @Transactional
    public void retirePreviousKeys() {
        SigningKey active = activeSigningKey();
        signingKeyRepository.findByRetiredFalse().stream()
                .filter(key -> !key.getKeyId().equals(active.getKeyId()))
                .forEach(key -> {
                    key.setRetired(true);
                    signingKeyRepository.save(key);
                });
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar novo par de chaves RSA", ex);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static RSAPublicKey parsePublicKey(String value) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(normalize(value));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Chave publica JWT invalida", ex);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String value) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(normalize(value));
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Chave privada JWT invalida", ex);
        }
    }

    private static String normalize(String value) {
        return value
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
    }
}
