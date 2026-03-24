package br.com.techchallenge.agendamento.controller;

import br.com.techchallenge.agendamento.dto.AtualizarConsultaRequest;
import br.com.techchallenge.agendamento.dto.CriarConsultaRequest;
import br.com.techchallenge.agendamento.entity.Consulta;
import br.com.techchallenge.agendamento.service.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public Consulta criar(@Valid @RequestBody CriarConsultaRequest request) {
        return consultaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public Consulta atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarConsultaRequest request) {
        return consultaService.atualizar(id, request);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<Consulta> minhasConsultas(Authentication authentication,
                                         @RequestParam(defaultValue = "false") boolean somenteFuturas) {
        return consultaService.consultarHistoricoPaciente(authentication.getName(), somenteFuturas);
    }
}
