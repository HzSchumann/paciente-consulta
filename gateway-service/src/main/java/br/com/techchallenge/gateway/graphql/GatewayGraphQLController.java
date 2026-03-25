package br.com.techchallenge.gateway.graphql;

import br.com.techchallenge.gateway.dto.AtualizarConsultaInput;
import br.com.techchallenge.gateway.dto.ConsultaGatewayType;
import br.com.techchallenge.gateway.dto.CriarConsultaInput;
import br.com.techchallenge.gateway.service.GatewayConsultaService;
import br.com.techchallenge.gateway.service.GatewayJwtRelayService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GatewayGraphQLController {

    private final GatewayConsultaService gatewayConsultaService;
    private final GatewayJwtRelayService gatewayJwtRelayService;

    public GatewayGraphQLController(GatewayConsultaService gatewayConsultaService,
                                    GatewayJwtRelayService gatewayJwtRelayService) {
        this.gatewayConsultaService = gatewayConsultaService;
        this.gatewayJwtRelayService = gatewayJwtRelayService;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    public List<ConsultaGatewayType> historicoPaciente(
            @Argument String pacienteUsername,
            @Argument Boolean somenteFuturas,
            Authentication authentication
    ) {
        return gatewayConsultaService.historicoPaciente(
                pacienteUsername,
                Boolean.TRUE.equals(somenteFuturas),
                gatewayJwtRelayService.bearerToken(authentication)
        );
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ConsultaGatewayType criarConsulta(@Argument CriarConsultaInput input, Authentication authentication) {
        return gatewayConsultaService.criarConsulta(input, gatewayJwtRelayService.bearerToken(authentication));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ConsultaGatewayType atualizarConsulta(@Argument Long id,
                                                 @Argument AtualizarConsultaInput input,
                                                 Authentication authentication) {
        return gatewayConsultaService.atualizarConsulta(id, input, gatewayJwtRelayService.bearerToken(authentication));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public Boolean cancelarConsulta(@Argument Long id, Authentication authentication) {
        return gatewayConsultaService.cancelarConsulta(id, gatewayJwtRelayService.bearerToken(authentication));
    }
}
