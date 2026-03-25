package br.com.techchallenge.auth.repository;

import br.com.techchallenge.auth.entity.SigningKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SigningKeyRepository extends JpaRepository<SigningKey, String> {
    Optional<SigningKey> findByActiveTrue();
    List<SigningKey> findByRetiredFalse();
}
