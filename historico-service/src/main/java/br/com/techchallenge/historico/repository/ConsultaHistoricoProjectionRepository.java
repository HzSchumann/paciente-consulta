package br.com.techchallenge.historico.repository;

import br.com.techchallenge.historico.entity.ConsultaHistoricoProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultaHistoricoProjectionRepository extends JpaRepository<ConsultaHistoricoProjection, Long> {

    List<ConsultaHistoricoProjection> findByPacienteUsernameOrderByDataHoraAsc(String pacienteUsername);

    List<ConsultaHistoricoProjection> findByPacienteUsernameAndDataHoraAfterOrderByDataHoraAsc(
            String pacienteUsername,
            LocalDateTime dataHora
    );

    List<ConsultaHistoricoProjection> findByPacienteUsernameAndStatusNotAndDataHoraAfterOrderByDataHoraAsc(
            String pacienteUsername,
            String status,
            LocalDateTime dataHora
    );
}
