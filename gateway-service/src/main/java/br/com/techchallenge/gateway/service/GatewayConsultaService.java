package br.com.techchallenge.gateway.service;

import br.com.techchallenge.gateway.config.DownstreamProperties;
import br.com.techchallenge.gateway.dto.AtualizarConsultaInput;
import br.com.techchallenge.gateway.dto.ConsultaGatewayType;
import br.com.techchallenge.gateway.dto.CriarConsultaInput;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GatewayConsultaService {

    private final DownstreamProperties downstreamProperties;
    private final RestClient restClient;

    public GatewayConsultaService(DownstreamProperties downstreamProperties) {
        this.downstreamProperties = downstreamProperties;
        this.restClient = RestClient.builder().build();
    }

    public ConsultaGatewayType criarConsulta(CriarConsultaInput input, String authorization) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pacienteUsername", input.pacienteUsername());
        body.put("medicoUsername", input.medicoUsername());
        body.put("enfermeiroUsername", input.enfermeiroUsername());
        body.put("dataHora", input.dataHora());
        body.put("observacoes", input.observacoes());
        JsonNode node = restClient.post()
                .uri(downstreamProperties.getAgendamentoBaseUrl() + "/consultas")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return fromAgendamentoNode(node);
    }

    public ConsultaGatewayType atualizarConsulta(Long id, AtualizarConsultaInput input, String authorization) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dataHora", input.dataHora());
        body.put("observacoes", input.observacoes());
        body.put("medicoUsername", input.medicoUsername());
        body.put("enfermeiroUsername", input.enfermeiroUsername());
        JsonNode node = restClient.put()
                .uri(downstreamProperties.getAgendamentoBaseUrl() + "/consultas/" + id)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return fromAgendamentoNode(node);
    }

    public boolean cancelarConsulta(Long id, String authorization) {
        restClient.delete()
                .uri(downstreamProperties.getAgendamentoBaseUrl() + "/consultas/" + id)
                .header("Authorization", authorization)
                .retrieve()
                .toBodilessEntity();
        return true;
    }

    public List<ConsultaGatewayType> historicoPaciente(String pacienteUsername, boolean somenteFuturas, String authorization) {
        String query = """
                query($pacienteUsername: String!, $somenteFuturas: Boolean) {
                  historicoPaciente(pacienteUsername: $pacienteUsername, somenteFuturas: $somenteFuturas) {
                    id
                    pacienteUsername
                    medicoUsername
                    enfermeiroUsername
                    dataHora
                    observacoes
                    status
                    ultimaAtualizacaoEm
                  }
                }
                """;
        JsonNode response = restClient.post()
                .uri(downstreamProperties.getHistoricoBaseUrl() + "/graphql")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "query", query,
                        "variables", Map.of(
                                "pacienteUsername", pacienteUsername,
                                "somenteFuturas", somenteFuturas
                        )
                ))
                .retrieve()
                .body(JsonNode.class);
        validateGraphQlResponse(response);

        List<ConsultaGatewayType> items = new ArrayList<>();
        JsonNode historicoNodes = response.path("data").path("historicoPaciente");
        historicoNodes.forEach(node -> items.add(new ConsultaGatewayType(
                node.path("id").asLong(),
                node.path("pacienteUsername").asText(),
                node.path("medicoUsername").asText(),
                textOrNull(node, "enfermeiroUsername"),
                parseDateTime(node, "dataHora"),
                textOrNull(node, "observacoes"),
                node.path("status").asText(),
                parseNullableDateTime(node, "ultimaAtualizacaoEm")
        )));
        return items;
    }

    private ConsultaGatewayType fromAgendamentoNode(JsonNode node) {
        return new ConsultaGatewayType(
                node.path("id").asLong(),
                node.path("pacienteUsername").asText(),
                node.path("medicoUsername").asText(),
                textOrNull(node, "enfermeiroUsername"),
                parseDateTime(node, "dataHora"),
                textOrNull(node, "observacoes"),
                node.path("status").asText(),
                null
        );
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void validateGraphQlResponse(JsonNode response) {
        JsonNode errors = response.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new IllegalStateException("Historico GraphQL retornou erro: " + errors.toString());
        }
        if (response.path("data").isMissingNode()) {
            throw new IllegalStateException("Historico GraphQL retornou resposta sem campo data");
        }
    }

    private LocalDateTime parseDateTime(JsonNode node, String field) {
        return LocalDateTime.parse(node.path(field).asText());
    }

    private LocalDateTime parseNullableDateTime(JsonNode node, String field) {
        String value = textOrNull(node, field);
        return value == null ? null : LocalDateTime.parse(value);
    }
}
