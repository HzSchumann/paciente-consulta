package br.com.techchallenge.auth.service;

import br.com.techchallenge.auth.entity.RefreshTokenSession;
import br.com.techchallenge.auth.entity.RevokedAccessToken;
import br.com.techchallenge.auth.repository.RefreshTokenSessionRepository;
import br.com.techchallenge.auth.repository.RevokedAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public RefreshTokenService(
            RefreshTokenSessionRepository refreshTokenSessionRepository,
            RevokedAccessTokenRepository revokedAccessTokenRepository
    ) {
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Transactional
    public String issue(String username, Instant expiresAt, String accessTokenJti) {
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        RefreshTokenSession session = new RefreshTokenSession();
        session.setTokenId(UUID.randomUUID().toString());
        session.setUsername(username);
        session.setTokenHash(hash(rawToken));
        session.setAccessTokenJti(accessTokenJti);
        session.setExpiresAt(expiresAt);
        refreshTokenSessionRepository.save(session);
        return session.getTokenId() + "." + rawToken;
    }

    @Transactional
    public RefreshTokenSession consume(String refreshToken) {
        String[] parts = refreshToken.split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Refresh token invalido");
        }
        RefreshTokenSession session = refreshTokenSessionRepository.findById(parts[0])
                .orElseThrow(() -> new IllegalArgumentException("Refresh token nao encontrado"));
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expirado ou revogado");
        }
        if (!session.getTokenHash().equals(hash(parts[1]))) {
            throw new IllegalArgumentException("Refresh token invalido");
        }
        revokeAccessToken(session.getAccessTokenJti(), session.getExpiresAt());
        session.setRevokedAt(Instant.now());
        refreshTokenSessionRepository.save(session);
        return session;
    }

    @Transactional
    public void revokeByRefreshToken(String refreshToken) {
        String[] parts = refreshToken.split("\\.", 2);
        if (parts.length != 2) {
            return;
        }
        refreshTokenSessionRepository.findById(parts[0]).ifPresent(session -> {
            revokeAccessToken(session.getAccessTokenJti(), session.getExpiresAt());
            session.setRevokedAt(Instant.now());
            refreshTokenSessionRepository.save(session);
        });
    }

    @Transactional
    public void revokeAccessToken(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || revokedAccessTokenRepository.existsById(jti)) {
            return;
        }
        RevokedAccessToken revoked = new RevokedAccessToken();
        revoked.setJti(jti);
        revoked.setExpiresAt(expiresAt);
        revoked.setRevokedAt(Instant.now());
        revokedAccessTokenRepository.save(revoked);
    }

    public boolean isRevoked(String jti) {
        return revokedAccessTokenRepository.existsById(jti);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel processar o refresh token", ex);
        }
    }
}
