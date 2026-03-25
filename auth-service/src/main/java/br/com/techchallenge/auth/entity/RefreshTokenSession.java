package br.com.techchallenge.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_token_sessions")
public class RefreshTokenSession {

    @Id
    private String tokenId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private String accessTokenJti;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private String replacedByTokenId;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getAccessTokenJti() {
        return accessTokenJti;
    }

    public void setAccessTokenJti(String accessTokenJti) {
        this.accessTokenJti = accessTokenJti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByTokenId() {
        return replacedByTokenId;
    }

    public void setReplacedByTokenId(String replacedByTokenId) {
        this.replacedByTokenId = replacedByTokenId;
    }
}
