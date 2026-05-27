package com.liber.controller;

import com.liber.dto.ExemplarRequest;
import com.liber.dto.ExemplarResponse;
import com.liber.service.ExemplarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestao avulsa dos exemplares de um livro (copias fisicas com codigo de
 * tombamento). Acoes especificas:
 *
 * <ul>
 *   <li>GET    /livros/{id}/exemplares       — lista exemplares do livro</li>
 *   <li>POST   /livros/{id}/exemplares       — adiciona 1 exemplar (codigo opcional)</li>
 *   <li>PUT    /exemplares/{id}              — renomeia o codigo</li>
 *   <li>POST   /exemplares/{id}/extraviar    — marca como extraviado</li>
 *   <li>POST   /exemplares/{id}/reativar     — volta de extraviado pra disponivel</li>
 *   <li>DELETE /exemplares/{id}              — remove (so se DISPONIVEL e sem historico)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Exemplares", description = "Copias fisicas dos livros (tombamento)")
public class ExemplarController {

    private final ExemplarService exemplarService;

    @GetMapping("/livros/{livroId}/exemplares")
    @Operation(summary = "Lista exemplares de um livro")
    public List<ExemplarResponse> listar(@PathVariable Long livroId) {
        return exemplarService.listarDoLivro(livroId);
    }

    @PostMapping("/livros/{livroId}/exemplares")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Adiciona um exemplar ao livro. Codigo opcional — gera default se vazio")
    public ResponseEntity<ExemplarResponse> adicionar(
            @PathVariable Long livroId,
            @Valid @RequestBody(required = false) ExemplarRequest req) {
        ExemplarResponse criado = exemplarService.adicionar(livroId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/exemplares/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Renomeia o codigo do exemplar (pra casar com etiqueta fisica)")
    public ExemplarResponse renomear(@PathVariable Long id,
                                     @Valid @RequestBody ExemplarRequest req) {
        return exemplarService.renomear(id, req);
    }

    @PostMapping("/exemplares/{id}/extraviar")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Marca o exemplar como EXTRAVIADO")
    public ExemplarResponse extraviar(@PathVariable Long id) {
        return exemplarService.marcarExtraviado(id);
    }

    @PostMapping("/exemplares/{id}/reativar")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Reativa exemplar extraviado (volta pra DISPONIVEL)")
    public ExemplarResponse reativar(@PathVariable Long id) {
        return exemplarService.reativar(id);
    }

    @DeleteMapping("/exemplares/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Remove o exemplar (so se DISPONIVEL e sem historico)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        exemplarService.remover(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
