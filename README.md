# Tech Challenge Fase 3 - Sistema de Pacientes e Consultas

Projeto backend modular com foco em segurança, GraphQL e comunicação assíncrona para ambiente hospitalar.

## Arquitetura

O repositório contém dois serviços:

1. **agendamento-service**
   - API REST para criar/editar consultas.
   - API GraphQL para consultar histórico do paciente.
   - Segurança com Spring Security (Basic Auth + RBAC).
   - Publicação de eventos no RabbitMQ quando consultas são criadas/editadas.

2. **notificacao-service**
   - Consumidor RabbitMQ.
   - Processa eventos de consulta e simula envio de lembrete ao paciente via log.

Infraestrutura:
- RabbitMQ (com painel de gerenciamento) via `docker-compose`.

## Regras de Acesso

- `ROLE_MEDICO`
  - Criar e editar consultas.
  - Consultar histórico via GraphQL.
- `ROLE_ENFERMEIRO`
  - Criar e editar consultas.
  - Consultar histórico via GraphQL.
- `ROLE_PACIENTE`
  - Endpoint REST `/consultas/minhas` para ver apenas suas consultas.
  - Query GraphQL retorna somente seu próprio histórico (ignora `pacienteUsername` informado).

Usuários de exemplo (Basic Auth):

- `medico1 / 123456`
- `enfermeiro1 / 123456`
- `paciente1 / 123456`
- `paciente2 / 123456`

## Execução

### 1) Subir RabbitMQ

```bash
docker compose up -d rabbitmq
```

Painel: `http://localhost:15672` (guest/guest).

### 2) Rodar os serviços

```bash
mvn -pl agendamento-service spring-boot:run
mvn -pl notificacao-service spring-boot:run
```

## Endpoints REST (agendamento-service)

Base URL: `http://localhost:8080`

- `POST /consultas` (MEDICO/ENFERMEIRO)
- `PUT /consultas/{id}` (MEDICO/ENFERMEIRO)
- `GET /consultas/minhas?somenteFuturas=true|false` (PACIENTE)

### Exemplo `POST /consultas`

```json
{
  "pacienteUsername": "paciente1",
  "medicoUsername": "medico1",
  "enfermeiroUsername": "enfermeiro1",
  "dataHora": "2030-01-10T14:00:00",
  "observacoes": "Retorno anual"
}
```

## GraphQL (agendamento-service)

- Endpoint: `POST /graphql`
- Interface GraphiQL: `http://localhost:8080/graphiql`

Query:

```graphql
query {
  historicoPaciente(pacienteUsername: "paciente1", somenteFuturas: true) {
    id
    pacienteUsername
    medicoUsername
    dataHora
    observacoes
  }
}
```

## Comunicação assíncrona

Quando uma consulta é criada ou editada no `agendamento-service`, um evento é publicado em:

- Exchange: `consulta.exchange`
- Routing key: `consulta.criada.editada`
- Queue: `consulta.notificacao.queue`

O `notificacao-service` consome esse evento e registra o lembrete no log.

## Testes e validação

- Collection do Postman: `postman/paciente-consulta.postman_collection.json`
- Recomenda-se testar:
  1. Criar consulta com médico/enfermeiro.
  2. Editar consulta.
  3. Consultar histórico por paciente (REST e GraphQL).
  4. Verificar logs do notificacao-service após criação/edição.
