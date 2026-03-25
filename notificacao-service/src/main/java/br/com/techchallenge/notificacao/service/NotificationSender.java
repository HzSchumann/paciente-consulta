package br.com.techchallenge.notificacao.service;

import br.com.techchallenge.contract.messaging.ConsultaEvent;

public interface NotificationSender {

    void enviar(ConsultaEvent event);
}
