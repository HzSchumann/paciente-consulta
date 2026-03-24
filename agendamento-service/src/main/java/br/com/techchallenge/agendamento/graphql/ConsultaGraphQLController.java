package br.com.techchallenge.agendamento.graphql;

import br.com.techchallenge.agendamento.entity.Consulta;
import br.com.techchallenge.agendamento.service.ConsultaService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ConsultaGraphQLController {

    private final ConsultaService consultaService;

    public ConsultaGraphQLController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    public List<Consulta> historicoPaciente(@Argument String pacienteUsername,
                                            @Argument Boolean somenteFuturas,
                                            Authentication authentication) {
        boolean futureOnly = Boolean.TRUE.equals(somenteFuturas);
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"))) {
            pacienteUsername = authentication.getName();
        }
        return consultaService.consultarHistoricoPaciente(pacienteUsername, futureOnly);
    }
}
