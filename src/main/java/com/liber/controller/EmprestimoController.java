package com.liber.controller;

import com.liber.dto.EditarEmprestimoRequest;
import com.liber.dto.EmprestimoRequest;
import com.liber.dto.EmprestimoResponse;
import com.liber.dto.RenovarEmprestimoRequest;
import com.liber.service.EmprestimoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/emprestimos")
@RequiredArgsConstructor
@Tag(name = "Emprestimos", description = "Registro e controle de emprestimos")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    @GetMapping("/ativos")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Lista emprestimos ATIVOS ordenados por urgencia (devolucao prevista mais proxima primeiro)")
    public List<EmprestimoResponse> listarAtivos() {
        return emprestimoService.listarAtivos();
    }

    @GetMapping("/historico")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Lista historico de emprestimos (mais recentes primeiro)")
    public Page<EmprestimoResponse> listarHistorico(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return emprestimoService.listarHistorico(pageable);
    }

    @GetMapping("/aluno/{alunoId}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Lista emprestimos de um aluno especifico")
    public Page<EmprestimoResponse> listarPorAluno(
            @PathVariable Long alunoId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return emprestimoService.listarPorAluno(alunoId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Busca emprestimo por id")
    public EmprestimoResponse buscar(@PathVariable Long id) {
        return emprestimoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Registra um novo emprestimo (decrementa estoque atomicamente)")
    public ResponseEntity<EmprestimoResponse> registrar(@Valid @RequestBody EmprestimoRequest req,
                                                        UriComponentsBuilder uri) {
        EmprestimoResponse criado = emprestimoService.registrar(req);
        return ResponseEntity
            .created(uri.path("/api/v1/emprestimos/{id}").buildAndExpand(criado.id()).toUri())
            .body(criado);
    }

    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Registra a devolucao de um emprestimo (incrementa estoque)")
    public ResponseEntity<EmprestimoResponse> registrarDevolucao(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(emprestimoService.registrarDevolucao(id));
    }

    @PostMapping("/{id}/renovacao")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Renova o prazo de um emprestimo ativo (limite e regras no service)")
    public ResponseEntity<EmprestimoResponse> renovar(@PathVariable Long id,
                                                      @Valid @RequestBody RenovarEmprestimoRequest req) {
        return ResponseEntity.ok(emprestimoService.renovar(id, req.prazoDias()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Edita campos de um emprestimo ativo (dataEmprestimo e/ou prazoDias)")
    public ResponseEntity<EmprestimoResponse> editar(@PathVariable Long id,
                                                     @Valid @RequestBody EditarEmprestimoRequest req) {
        return ResponseEntity.ok(emprestimoService.editar(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Cancela um emprestimo (lancamento errado) — livro volta ao estoque")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        emprestimoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Remove permanentemente um emprestimo do historico (so se DEVOLVIDO ou CANCELADO)")
    public ResponseEntity<Void> removerDoHistorico(@PathVariable Long id) {
        emprestimoService.removerDoHistorico(id);
        return ResponseEntity.noContent().build();
    }
}
