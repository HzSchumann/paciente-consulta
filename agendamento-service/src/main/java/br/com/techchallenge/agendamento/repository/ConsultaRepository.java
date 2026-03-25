package br.com.techchallenge.agendamento.repository;

import br.com.techchallenge.agendamento.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByPacienteUsername(String pacienteUsername);

    List<Consulta> findByPacienteUsernameAndDataHoraAfter(String pacienteUsername, LocalDateTime dataHora);
}
