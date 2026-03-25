package br.com.techchallenge.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "downstream")
public class DownstreamProperties {

    private String agendamentoBaseUrl;
    private String historicoBaseUrl;

    public String getAgendamentoBaseUrl() {
        return agendamentoBaseUrl;
    }

    public void setAgendamentoBaseUrl(String agendamentoBaseUrl) {
        this.agendamentoBaseUrl = agendamentoBaseUrl;
    }

    public String getHistoricoBaseUrl() {
        return historicoBaseUrl;
    }

    public void setHistoricoBaseUrl(String historicoBaseUrl) {
        this.historicoBaseUrl = historicoBaseUrl;
    }
}
