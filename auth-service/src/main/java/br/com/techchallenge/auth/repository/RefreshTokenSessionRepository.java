package br.com.techchallenge.auth.repository;

import br.com.techchallenge.auth.entity.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, String> {
}
