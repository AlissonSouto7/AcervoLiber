package com.liber.controller;

import com.liber.dto.AlunoRequest;
import com.liber.dto.AlunoResponse;
import com.liber.dto.auth.CriarAcessoAlunoRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/alunos")
@RequiredArgsConstructor
@Tag(name = "Alunos", description = "Cadastro de alunos")
public class AlunoController {

    private final AlunoService alunoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Lista alunos com paginacao e busca")
    public Page<AlunoResponse> listar(
            @Parameter(description = "Busca em nome, matricula ou turma")
            @RequestParam(required = false) String termo,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return alunoService.listar(termo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Busca aluno por id")
    public AlunoResponse buscar(@PathVariable Long id) {
        return alunoService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Cadastra um novo aluno")
    public ResponseEntity<AlunoResponse> cadastrar(@Valid @RequestBody AlunoRequest req,
                                                   UriComponentsBuilder uri) {
        AlunoResponse criado = alunoService.cadastrar(req);
        return ResponseEntity
            .created(uri.path("/api/v1/alunos/{id}").buildAndExpand(criado.id()).toUri())
            .body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Atualiza um aluno existente")
    public AlunoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AlunoRequest req) {
        return alunoService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Remove um aluno (apenas se nao tiver historico de emprestimos)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        alunoService.remover(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/acesso")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Cria o acesso de login do aluno (senha provisoria — exige troca no 1o acesso)")
    public ResponseEntity<UsuarioResponse> criarAcesso(@PathVariable Long id,
                                                       @Valid @RequestBody CriarAcessoAlunoRequest req) {
        UsuarioResponse acesso = alunoService.criarAcesso(id, req.senhaInicial());
        return ResponseEntity.status(HttpStatus.CREATED).body(acesso);
    }
}
