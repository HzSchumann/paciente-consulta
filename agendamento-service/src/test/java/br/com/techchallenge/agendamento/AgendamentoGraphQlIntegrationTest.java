package br.com.techchallenge.agendamento;

import br.com.techchallenge.agendamento.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.outbox.publisher.enabled=false")
@AutoConfigureMockMvc
class AgendamentoGraphQlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void mutationsFuncionamESchemaNaoExibeEntidadeJPA() throws Exception {
        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer("medico1", "MEDICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"mutation { criarConsulta(input: { pacienteUsername: \\"paciente1\\", medicoUsername: \\"medico1\\", enfermeiroUsername: \\"enfermeiro1\\", dataHora: \\"2030-01-10T14:00:00\\", observacoes: \\"Mutacao\\" }) { id pacienteUsername observacoes } }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criarConsulta.pacienteUsername").value("paciente1"));

        mockMvc.perform(post("/graphql")
                        .header("Authorization", bearer("medico1", "MEDICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"query { serviceInfo }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceInfo").value("agendamento-service"));

        assertThat(outboxEventRepository.findAll()).isNotEmpty();
    }

    private String bearer(String username, String... roles) {
        return JwtTestTokens.bearer(username, roles);
    }
}
