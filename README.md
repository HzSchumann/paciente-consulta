# Tech Challenge Fase 3 - Sistema de Pacientes e Consultas.

Projeto backend multi-modulo em Spring Boot com separacao entre identidade, comando, leitura projetada, notificacao assincrona e BFF GraphQL.

## Modulos

- `consulta-contract`
  - Contrato compartilhado dos eventos AMQP.
- `auth-service`
  - Identidade e autenticacao.
  - Emissao de `accessToken` e `refreshToken`.
  - Rotacao de chaves `RS256` com JWKS.
  - Revogacao de access token e refresh token.
  - Provisionamento administrativo de usuarios.
- `agendamento-service`
  - Comando de consultas.
  - REST e GraphQL para criar, atualizar e cancelar consultas.
  - Outbox transacional com claim, lock e fencing token.
- `historico-service`
  - Projecao de leitura do historico.
  - Consumo idempotente de eventos por `eventId`.
- `notificacao-service`
  - Consumo idempotente de eventos para notificacao.
- `gateway-service`
  - BFF GraphQL unificado para queries de historico e mutations de consulta.
- `system-tests`
  - Teste ponta a ponta com Testcontainers e RabbitMQ real.

## Arquitetura

Fluxo principal:

1. Cliente autentica no `auth-service`.
2. Admin provisiona usuarios em `POST /admin/users`.
3. Cliente cria, atualiza ou cancela consulta por REST ou GraphQL.
4. `agendamento-service` persiste consulta e evento no outbox na mesma transacao.
5. `OutboxPublisher` faz claim por lote, usa lock pessimista e aplica fencing por `claimToken`.
6. `historico-service` projeta o read model e ignora duplicados por `eventId`.
7. `notificacao-service` consome o mesmo evento e ignora duplicados por `eventId`.
8. `gateway-service` expõe um contrato GraphQL unificado para leitura e comando.

Topologia RabbitMQ:

- Exchange: `consulta.exchange`
- Routing key: `consulta.criada.editada`
- Queue leitura: `consulta.historico.queue`
- Queue notificacao: `consulta.notificacao.queue`
- DLX: `consulta.dlx`

## Seguranca

- Autenticacao stateless com JWT `RS256`.
- Chaves publicas expostas em `GET /.well-known/jwks.json` no `auth-service`.
- `agendamento-service`, `historico-service` e `gateway-service` validam tokens via JWKS e consultam revogacao interna.
- Roles suportadas:
  - `ADMIN`
  - `MEDICO`
  - `ENFERMEIRO`
  - `PACIENTE`

Regras principais:

- `MEDICO` e `ENFERMEIRO`: criam, atualizam e cancelam consultas.
- `PACIENTE`: consulta apenas o proprio historico.
- `ADMIN`: provisiona usuarios e opera rotacao de chaves.

Observacoes:

- A identidade principal nao fica mais no `agendamento-service`.
- O `auth-service` usa persistencia propria para usuarios, refresh tokens, tokens revogados e chaves de assinatura.
- O endpoint `DELETE /consultas/{id}` representa cancelamento logico, nao exclusao fisica.

## Como Rodar

### 1. Configure o bootstrap admin

```bash
export APP_BOOTSTRAP_ADMIN_USERNAME=admin
export APP_BOOTSTRAP_ADMIN_PASSWORD=admin123
export APP_BOOTSTRAP_ADMIN_ROLES=ADMIN
export APP_INTERNAL_AUTH_SECRET=change-me-now
```

### 2. Suba a infraestrutura

```bash
docker compose up -d postgres rabbitmq
```

### 3. Suba os servicos

```bash
mvn -pl auth-service spring-boot:run
mvn -pl agendamento-service spring-boot:run
mvn -pl historico-service spring-boot:run
mvn -pl notificacao-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

Portas:

- `auth-service`: `http://localhost:8084`
- `agendamento-service`: `http://localhost:8080`
- `historico-service`: `http://localhost:8082`
- `gateway-service`: `http://localhost:8083`
- RabbitMQ Management: `http://localhost:15672`
- PostgreSQL: `localhost:5432`

Datasources default:

- `auth-service`: `jdbc:postgresql://localhost:5432/agendamento`
- `agendamento-service`: `jdbc:postgresql://localhost:5432/agendamento`

## APIs

### `auth-service`

REST:

- `POST /auth/token`
- `POST /auth/refresh`
- `POST /auth/revoke`
- `POST /admin/users`
- `POST /admin/keys/rotate`
- `POST /admin/keys/retire-previous`
- `GET /.well-known/jwks.json`
- `GET /internal/tokens/revoked/{jti}`

Resposta de token:

```json
{
  "accessToken": "eyJraWQiOiJraWQt...\"",
  "refreshToken": "session-id.token"
}
```

Provisionamento de usuario:

```json
{
  "username": "medico1",
  "password": "123456",
  "roles": "MEDICO"
}
```

Refresh:

```json
{
  "refreshToken": "session-id.token"
}
```

Revogacao:

```json
{
  "refreshToken": "session-id.token"
}
```

### `agendamento-service`

REST:

- `POST /consultas`
- `PUT /consultas/{id}`
- `DELETE /consultas/{id}`

Criacao de consulta:

```json
{
  "pacienteUsername": "paciente1",
  "medicoUsername": "medico1",
  "enfermeiroUsername": "enfermeiro1",
  "dataHora": "2030-01-10T14:00:00",
  "observacoes": "Retorno anual"
}
```

Cancelamento:

```text
DELETE /consultas/{id}
```

Semantica:

- O delete marca a consulta como `CANCELADA`.
- O historico continua existindo e passa a refletir o status cancelado.

GraphQL:

- `POST /graphql`
- `GET /graphiql`

```graphql
mutation {
  criarConsulta(
    input: {
      pacienteUsername: "paciente1"
      medicoUsername: "medico1"
      enfermeiroUsername: "enfermeiro1"
      dataHora: "2030-01-10T14:00:00"
      observacoes: "Retorno anual"
    }
  ) {
    id
    status
    dataHora
  }
}
```

### `historico-service`

GraphQL:

- `POST /graphql`
- `GET /graphiql`

```graphql
query {
  historicoPaciente(pacienteUsername: "paciente1", somenteFuturas: true) {
    id
    status
    dataHora
    ultimaAtualizacaoEm
  }
}
```

Se o token for de `PACIENTE`, o username informado e ignorado e substituido pelo usuario autenticado.

### `gateway-service`

GraphQL unificado:

- `POST /graphql`
- `GET /graphiql`

```graphql
query {
  historicoPaciente(pacienteUsername: "paciente1", somenteFuturas: false) {
    id
    status
    dataHora
  }
}
```

```graphql
mutation {
  cancelarConsulta(id: 1)
}
```

## GraphQL DateTime

Os schemas usam o scalar customizado `DateTime` com formato ISO-8601 local:

```text
2030-01-10T14:00:00
```

## Testes

Executar testes de modulo:

```bash
mvn -q -pl auth-service,agendamento-service,historico-service,gateway-service -am test
```

Executar E2E:

```bash
mvn -q -pl system-tests -am test
```

Cobertura atual inclui:

- autorizacao por role
- mutations GraphQL
- projection GraphQL
- outbox publisher com fencing
- retry/DLQ de mensageria
- rotacao de chaves, refresh e revogacao no `auth-service`
- idempotencia em historico e notificacao
- fluxo E2E com RabbitMQ real via Testcontainers

Observacao:

- A suite `system-tests` depende de Docker disponivel localmente.
