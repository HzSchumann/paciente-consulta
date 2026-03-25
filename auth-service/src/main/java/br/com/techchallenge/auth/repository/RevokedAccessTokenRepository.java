package br.com.techchallenge.auth.repository;

import br.com.techchallenge.auth.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {
}
