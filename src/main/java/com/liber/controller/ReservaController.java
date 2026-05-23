package com.liber.controller;

import com.liber.dto.ConfirmarReservaRequest;
import com.liber.dto.CriarReservaRequest;
import com.liber.dto.ReservaResponse;
import com.liber.dto.ReservaResumoResponse;
import com.liber.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reserva de livros — alunos reservam, bibliotecarios confirmam")
public class ReservaController {

    private final ReservaService reservaService;

    // ---------- Aluno ----------

    @PostMapping
    @PreAuthorize("hasRole('ALUNO')")
    @Operation(summary = "Aluno reserva um livro disponivel")
    public ResponseEntity<ReservaResponse> reservar(@AuthenticationPrincipal UserDetails principal,
                                                    @Valid @RequestBody CriarReservaRequest req,
                                                    UriComponentsBuilder uri) {
        ReservaResponse criada = reservaService.reservar(principal.getUsername(), req.livroId());
        return ResponseEntity
            .created(uri.path("/api/v1/reservas/{id}").buildAndExpand(criada.id()).toUri())
            .body(criada);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('ALUNO')")
    @Operation(summary = "Lista as reservas do aluno autenticado")
    public Page<ReservaResponse> minhas(@AuthenticationPrincipal UserDetails principal,
                                        @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return reservaService.listarMinhas(principal.getUsername(), pageable);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ALUNO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Aluno cancela uma reserva pendente sua")
    public void cancelar(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        reservaService.cancelar(principal.getUsername(), id);
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('ALUNO')")
    @Operation(summary = "Resumo de emprestimos/reservas do aluno e o limite")
    public ReservaResumoResponse resumo(@AuthenticationPrincipal UserDetails principal) {
        return reservaService.resumoDoAluno(principal.getUsername());
    }

    // ---------- Bibliotecario ----------

    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Lista as reservas pendentes (fila do bibliotecario), paginada")
    public Page<ReservaResponse> pendentes(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return reservaService.listarPendentes(pageable);
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Confirma uma reserva — gera o emprestimo")
    public ReservaResponse confirmar(@PathVariable Long id,
                                     @Valid @RequestBody ConfirmarReservaRequest req) {
        return reservaService.confirmar(id, req.prazoDias());
    }

    @PostMapping("/{id}/recusar")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Recusa uma reserva — libera o exemplar")
    public ReservaResponse recusar(@PathVariable Long id) {
        return reservaService.recusar(id);
    }
}
