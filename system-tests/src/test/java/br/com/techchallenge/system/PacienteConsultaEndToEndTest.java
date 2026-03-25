package br.com.techchallenge.system;

import br.com.techchallenge.auth.AuthServiceApplication;
import br.com.techchallenge.agendamento.AgendamentoServiceApplication;
import br.com.techchallenge.agendamento.entity.OutboxStatus;
import br.com.techchallenge.agendamento.repository.OutboxEventRepository;
import br.com.techchallenge.agendamento.service.OutboxPublisher;
import br.com.techchallenge.gateway.GatewayServiceApplication;
import br.com.techchallenge.historico.HistoricoServiceApplication;
import br.com.techchallenge.historico.repository.ConsultaHistoricoProjectionRepository;
import br.com.techchallenge.historico.repository.ProcessedHistoricoEventRepository;
import br.com.techchallenge.notificacao.NotificacaoServiceApplication;
import br.com.techchallenge.notificacao.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PacienteConsultaEndToEndTest {

    private static final String ADMIN_PASSWORD = "admin123";
    private static final String INTERNAL_SECRET = "system-test-internal-secret";

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final RestClient REST_CLIENT = RestClient.builder().build();

    private static ConfigurableApplicationContext agendamentoContext;
    private static ConfigurableApplicationContext historicoContext;
    private static ConfigurableApplicationContext notificacaoContext;
    private static ConfigurableApplicationContext gatewayContext;
    private static ConfigurableApplicationContext authContext;

    @BeforeAll
    static void setUp() {
        rabbitMQ.start();

        authContext = startApp(AuthServiceApplication.class,
                "server.port=18084",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration,org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration,org.springframework.boot.autoconfigure.graphql.security.GraphQlWebMvcSecurityAutoConfiguration",
                "spring.datasource.url=jdbc:h2:mem:authe2e;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "app.security.internal-secret=" + INTERNAL_SECRET,
                "app.security.bootstrap-admin.username=admin",
                "app.security.bootstrap-admin.password=" + ADMIN_PASSWORD,
                "app.security.bootstrap-admin.roles=ADMIN"
        );

        notificacaoContext = startApp(NotificacaoServiceApplication.class,
                "server.port=18081",
                "spring.main.web-application-type=none",
                "spring.graphql.schema.locations=classpath:graphql/notificacao/",
                "spring.rabbitmq.host=" + rabbitMQ.getHost(),
                "spring.rabbitmq.port=" + rabbitMQ.getAmqpPort(),
                "spring.datasource.url=jdbc:h2:mem:notificacaoe2e;DB_CLOSE_DELAY=-1"
        );

        historicoContext = startApp(HistoricoServiceApplication.class,
                "server.port=18082",
                "spring.graphql.schema.locations=classpath:graphql/historico/",
                "spring.rabbitmq.host=" + rabbitMQ.getHost(),
                "spring.rabbitmq.port=" + rabbitMQ.getAmqpPort(),
                "spring.datasource.url=jdbc:h2:mem:historicoe2e;DB_CLOSE_DELAY=-1",
                "spring.rabbitmq.listener.simple.auto-startup=true",
                "app.security.jwt.jwk-set-uri=http://localhost:18084/.well-known/jwks.json",
                "app.security.jwt.token-status-uri=http://localhost:18084/internal/tokens/revoked",
                "app.security.jwt.internal-secret=" + INTERNAL_SECRET
        );

        agendamentoContext = startApp(AgendamentoServiceApplication.class,
                "server.port=18080",
                "spring.graphql.schema.locations=classpath:graphql/agendamento/",
                "spring.rabbitmq.host=" + rabbitMQ.getHost(),
                "spring.rabbitmq.port=" + rabbitMQ.getAmqpPort(),
                "spring.datasource.url=jdbc:h2:mem:agendamentoe2e;DB_CLOSE_DELAY=-1",
                "app.security.jwt.jwk-set-uri=http://localhost:18084/.well-known/jwks.json",
                "app.security.jwt.token-status-uri=http://localhost:18084/internal/tokens/revoked",
                "app.security.jwt.internal-secret=" + INTERNAL_SECRET,
                "app.outbox.publisher.fixed-delay=200"
        );

        gatewayContext = startApp(GatewayServiceApplication.class,
                "server.port=18083",
                "spring.graphql.schema.locations=classpath:graphql/gateway/",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
                "app.security.jwt.jwk-set-uri=http://localhost:18084/.well-known/jwks.json",
                "app.security.jwt.token-status-uri=http://localhost:18084/internal/tokens/revoked",
                "app.security.jwt.internal-secret=" + INTERNAL_SECRET,
                "downstream.agendamento-base-url=http://localhost:18080",
                "downstream.historico-base-url=http://localhost:18082"
        );
    }

    @AfterAll
    static void tearDown() {
        close(gatewayContext);
        close(agendamentoContext);
        close(historicoContext);
        close(notificacaoContext);
        close(authContext);
        rabbitMQ.stop();
    }

    @Test
    void fluxoCompletoPublicaProjetaNotificaECancela() throws Exception {
        String adminToken = token("admin", ADMIN_PASSWORD);
        provisionUser(adminToken, "medico1", "123456", "MEDICO");
        provisionUser(adminToken, "paciente1", "123456", "PACIENTE");

        String medicoToken = token("medico1", "123456");
        String pacienteToken = token("paciente1", "123456");

        JsonNode createResponse = graphQl("http://localhost:18083/graphql", medicoToken, """
                {
                  "query":"mutation { criarConsulta(input: { pacienteUsername: \\"paciente1\\", medicoUsername: \\"medico1\\", enfermeiroUsername: \\"enfermeiro1\\", dataHora: \\"2030-01-10T14:00:00\\", observacoes: \\"E2E\\" }) { id status pacienteUsername } }"
                }
                """);

        long consultaId = createResponse.path("data").path("criarConsulta").path("id").asLong();
        assertThat(createResponse.path("data").path("criarConsulta").path("status").asText()).isEqualTo("AGENDADA");

        OutboxPublisher outboxPublisher = agendamentoContext.getBean(OutboxPublisher.class);
        outboxPublisher.publicarPendentesManualmente();

        awaitUntil(() -> agendamentoContext.getBean(OutboxEventRepository.class).findAll().stream()
                .anyMatch(event -> event.getStatus() == OutboxStatus.PUBLISHED), Duration.ofSeconds(10));
        awaitUntil(() -> historicoContext.getBean(ProcessedHistoricoEventRepository.class).count() >= 1, Duration.ofSeconds(20));
        awaitUntil(() -> historicoContext.getBean(ConsultaHistoricoProjectionRepository.class).count() >= 1, Duration.ofSeconds(20));

        AtomicReference<JsonNode> ultimoHistorico = new AtomicReference<>();
        awaitUntil(() -> {
            JsonNode historico = graphQl("http://localhost:18083/graphql", pacienteToken, """
                    {
                      "query":"query { historicoPaciente(pacienteUsername: \\"paciente1\\", somenteFuturas: false) { id status pacienteUsername } }"
                    }
                    """);
            ultimoHistorico.set(historico);
            return historico.path("data").path("historicoPaciente").isArray()
                    && historico.path("data").path("historicoPaciente").size() == 1
                    && historico.path("data").path("historicoPaciente").get(0).path("status").asText().equals("AGENDADA");
        }, Duration.ofSeconds(20));
        assertThat(ultimoHistorico.get().path("data").path("historicoPaciente").size()).isEqualTo(1);

        awaitUntil(() -> notificacaoContext.getBean(ProcessedEventRepository.class).count() >= 1, Duration.ofSeconds(20));

        JsonNode cancelResponse = graphQl("http://localhost:18083/graphql", medicoToken, """
                {
                  "query":"mutation { cancelarConsulta(id: %d) }"
                }
                """.formatted(consultaId));
        assertThat(cancelResponse.path("data").path("cancelarConsulta").asBoolean()).isTrue();

        outboxPublisher.publicarPendentesManualmente();
        awaitUntil(() -> historicoContext.getBean(ProcessedHistoricoEventRepository.class).count() >= 2, Duration.ofSeconds(20));

        awaitUntil(() -> {
            JsonNode historico = graphQl("http://localhost:18083/graphql", pacienteToken, """
                    {
                      "query":"query { historicoPaciente(pacienteUsername: \\"paciente1\\", somenteFuturas: false) { id status } }"
                    }
                    """);
            JsonNode items = historico.path("data").path("historicoPaciente");
            return items.isArray()
                    && items.size() == 1
                    && items.get(0).path("status").asText().equals("CANCELADA");
        }, Duration.ofSeconds(20));

        JsonNode futuras = graphQl("http://localhost:18083/graphql", pacienteToken, """
                {
                  "query":"query { historicoPaciente(pacienteUsername: \\"paciente1\\", somenteFuturas: true) { id } }"
                }
                """);
        assertThat(futuras.path("data").path("historicoPaciente").size()).isZero();
    }

    private static ConfigurableApplicationContext startApp(Class<?> appClass, String... properties) {
        return new SpringApplicationBuilder(appClass)
                .run(toArgs(properties));
    }

    private static String[] toArgs(String... properties) {
        String[] args = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            args[i] = "--" + properties[i];
        }
        return args;
    }

    private static String token(String username, String password) {
        JsonNode response = REST_CLIENT.post()
                .uri("http://localhost:18084/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "username":"%s",
                          "password":"%s"
                        }
                        """.formatted(username, password))
                .retrieve()
                .body(JsonNode.class);
        JsonNode accessToken = response.get("accessToken");
        if (accessToken != null && !accessToken.isNull()) {
            return accessToken.asText();
        }
        return response.path("token").asText();
    }

    private static void provisionUser(String adminToken, String username, String password, String roles) {
        REST_CLIENT.post()
                .uri("http://localhost:18084/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "username":"%s",
                          "password":"%s",
                          "roles":"%s"
                        }
                        """.formatted(username, password, roles))
                .retrieve()
                .toBodilessEntity();
    }

    private static JsonNode graphQl(String url, String token, String body) throws Exception {
        String response = REST_CLIENT.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return OBJECT_MAPPER.readTree(response);
    }

    private static void awaitUntil(Check check, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (check.evaluate()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Timeout aguardando condição E2E");
    }

    private static void close(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }

    @FunctionalInterface
    interface Check {
        boolean evaluate() throws Exception;
    }
}
