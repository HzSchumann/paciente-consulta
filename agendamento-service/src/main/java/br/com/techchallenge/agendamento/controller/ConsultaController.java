package br.com.techchallenge.agendamento.controller;

import br.com.techchallenge.agendamento.dto.AtualizarConsultaRequest;
import br.com.techchallenge.agendamento.dto.ConsultaResponse;
import br.com.techchallenge.agendamento.dto.CriarConsultaRequest;
import br.com.techchallenge.agendamento.service.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultaResponse criar(@Valid @RequestBody CriarConsultaRequest request) {
        return consultaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ConsultaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarConsultaRequest request) {
        return consultaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id, Authentication authentication) {
        consultaService.cancelar(id, authentication.getName());
    }
}
