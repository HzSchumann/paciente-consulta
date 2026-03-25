package br.com.techchallenge.contract.messaging;

public final class ConsultaMessagingTopology {

    public static final String EXCHANGE_CONSULTA = "consulta.exchange";
    public static final String QUEUE_NOTIFICACAO = "consulta.notificacao.queue";
    public static final String QUEUE_HISTORICO = "consulta.historico.queue";
    public static final String ROUTING_KEY_CONSULTA = "consulta.criada.editada";
    public static final String DEAD_LETTER_EXCHANGE = "consulta.dlx";
    public static final String NOTIFICACAO_DEAD_LETTER_QUEUE = "consulta.notificacao.dlq";
    public static final String NOTIFICACAO_DEAD_LETTER_ROUTING_KEY = "consulta.notificacao.erro";
    public static final String HISTORICO_DEAD_LETTER_QUEUE = "consulta.historico.dlq";
    public static final String HISTORICO_DEAD_LETTER_ROUTING_KEY = "consulta.historico.erro";

    private ConsultaMessagingTopology() {
    }
}
