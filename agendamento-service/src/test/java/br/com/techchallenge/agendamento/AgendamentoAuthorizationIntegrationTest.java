package br.com.techchallenge.agendamento;

import br.com.techchallenge.agendamento.entity.Consulta;
import br.com.techchallenge.agendamento.entity.OutboxEvent;
import br.com.techchallenge.agendamento.repository.ConsultaRepository;
import br.com.techchallenge.agendamento.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.outbox.publisher.enabled=false")
@AutoConfigureMockMvc
class AgendamentoAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        consultaRepository.deleteAll();
    }

    @Test
    void medicoPodeCriarConsulta() throws Exception {
        mockMvc.perform(post("/consultas")
                        .header("Authorization", bearer("medico1", "MEDICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteUsername":"paciente1",
                                  "medicoUsername":"medico1",
                                  "enfermeiroUsername":"enfermeiro1",
                                  "dataHora":"2030-01-10T14:00:00",
                                  "observacoes":"Consulta criada"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteUsername").value("paciente1"));

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        assertThat(consultaRepository.findAll()).hasSize(1);
    }

    @Test
    void enfermeiroPodeAtualizarConsulta() throws Exception {
        Consulta consulta = new Consulta();
        consulta.setPacienteUsername("paciente1");
        consulta.setMedicoUsername("medico1");
        consulta.setEnfermeiroUsername("enfermeiro1");
        consulta.setDataHora(LocalDateTime.of(2030, 1, 10, 14, 0));
        consulta.setObservacoes("Original");
        consulta.setStatus(br.com.techchallenge.agendamento.entity.ConsultaStatus.AGENDADA);
        consulta = consultaRepository.save(consulta);

        mockMvc.perform(put("/consultas/{id}", consulta.getId())
                        .header("Authorization", bearer("enfermeiro1", "ENFERMEIRO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observacoes":"Atualizada pelo enfermeiro",
                                  "dataHora":"2030-01-10T15:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Atualizada pelo enfermeiro"));

        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }

    @Test
    void pacienteNaoPodeExecutarComandosDeConsulta() throws Exception {
        Consulta consulta = new Consulta();
        consulta.setPacienteUsername("paciente1");
        consulta.setMedicoUsername("medico1");
        consulta.setEnfermeiroUsername("enfermeiro1");
        consulta.setDataHora(LocalDateTime.of(2030, 1, 10, 14, 0));
        consulta.setObservacoes("Consulta do paciente");
        consulta.setStatus(br.com.techchallenge.agendamento.entity.ConsultaStatus.AGENDADA);
        consultaRepository.save(consulta);

        mockMvc.perform(post("/consultas")
                        .header("Authorization", bearer("paciente1", "PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteUsername":"paciente1",
                                  "medicoUsername":"medico1",
                                  "dataHora":"2030-01-10T14:00:00"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/consultas/{id}", consulta.getId())
                        .header("Authorization", bearer("paciente1", "PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observacoes":"Tentativa indevida"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String bearer(String username, String... roles) {
        return JwtTestTokens.bearer(username, roles);
    }
}
