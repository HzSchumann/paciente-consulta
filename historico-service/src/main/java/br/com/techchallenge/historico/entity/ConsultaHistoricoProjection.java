package br.com.techchallenge.historico.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "consulta_historico_projection")
public class ConsultaHistoricoProjection {

    @Id
    private Long id;
    private String pacienteUsername;
    private String medicoUsername;
    private String enfermeiroUsername;
    private LocalDateTime dataHora;
    private String observacoes;
    private String status;
    private LocalDateTime ultimaAtualizacaoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPacienteUsername() {
        return pacienteUsername;
    }

    public void setPacienteUsername(String pacienteUsername) {
        this.pacienteUsername = pacienteUsername;
    }

    public String getMedicoUsername() {
        return medicoUsername;
    }

    public void setMedicoUsername(String medicoUsername) {
        this.medicoUsername = medicoUsername;
    }

    public String getEnfermeiroUsername() {
        return enfermeiroUsername;
    }

    public void setEnfermeiroUsername(String enfermeiroUsername) {
        this.enfermeiroUsername = enfermeiroUsername;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUltimaAtualizacaoEm() {
        return ultimaAtualizacaoEm;
    }

    public void setUltimaAtualizacaoEm(LocalDateTime ultimaAtualizacaoEm) {
        this.ultimaAtualizacaoEm = ultimaAtualizacaoEm;
    }
}
