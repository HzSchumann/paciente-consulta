package br.com.techchallenge.agendamento.graphql;

import br.com.techchallenge.agendamento.dto.AtualizarConsultaInput;
import br.com.techchallenge.agendamento.dto.CriarConsultaInput;
import br.com.techchallenge.agendamento.dto.ConsultaGraphQlType;
import br.com.techchallenge.agendamento.mapper.ConsultaMapper;
import br.com.techchallenge.agendamento.service.ConsultaService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;

@Controller
public class ConsultaGraphQLController {

    private final ConsultaService consultaService;
    private final ConsultaMapper consultaMapper;

    public ConsultaGraphQLController(ConsultaService consultaService, ConsultaMapper consultaMapper) {
        this.consultaService = consultaService;
        this.consultaMapper = consultaMapper;
    }

    @QueryMapping
    public String serviceInfo() {
        return "agendamento-service";
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ConsultaGraphQlType criarConsulta(@Valid @Argument("input") CriarConsultaInput input) {
        return consultaMapper.toGraphQlType(consultaService.criar(input.toRequest()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ConsultaGraphQlType atualizarConsulta(@Argument Long id, @Valid @Argument("input") AtualizarConsultaInput input) {
        return consultaMapper.toGraphQlType(consultaService.atualizar(id, input.toRequest()));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public Boolean cancelarConsulta(@Argument Long id, Authentication authentication) {
        consultaService.cancelar(id, authentication.getName());
        return true;
    }
}
