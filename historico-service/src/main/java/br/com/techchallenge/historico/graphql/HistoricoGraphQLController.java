package br.com.techchallenge.historico.graphql;

import br.com.techchallenge.historico.dto.ConsultaHistoricoGraphQlType;
import br.com.techchallenge.historico.service.HistoricoConsultaService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class HistoricoGraphQLController {

    private final HistoricoConsultaService historicoConsultaService;

    public HistoricoGraphQLController(HistoricoConsultaService historicoConsultaService) {
        this.historicoConsultaService = historicoConsultaService;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    public List<ConsultaHistoricoGraphQlType> historicoPaciente(
            @Argument String pacienteUsername,
            @Argument Boolean somenteFuturas,
            Authentication authentication
    ) {
        String pacienteFiltrado = pacienteUsername;
        if (authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_PACIENTE"))) {
            pacienteFiltrado = authentication.getName();
        }
        return historicoConsultaService.consultarHistoricoPaciente(pacienteFiltrado, Boolean.TRUE.equals(somenteFuturas));
    }
}
