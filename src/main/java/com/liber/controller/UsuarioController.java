package com.liber.controller;

import com.liber.dto.auth.AtualizarStatusUsuarioRequest;
import com.liber.dto.auth.CriarUsuarioRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.service.UsuarioService;
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
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios (admin)", description = "Gestao de usuarios — apenas ADMIN")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Lista usuarios")
    public Page<UsuarioResponse> listar(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return usuarioService.listar(pageable);
    }

    @PostMapping
    @Operation(summary = "Cria um novo usuario com a role escolhida")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest req,
                                                  UriComponentsBuilder uri) {
        UsuarioResponse criado = usuarioService.criarComoAdmin(req);
        return ResponseEntity
            .created(uri.path("/api/v1/usuarios/{id}").buildAndExpand(criado.id()).toUri())
            .body(criado);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa ou desativa um usuario (admin nao pode desativar a si mesmo)")
    public UsuarioResponse alterarStatus(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AtualizarStatusUsuarioRequest req) {
        return usuarioService.alterarStatus(id, req.ativo(), principal.getUsername());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um usuario permanentemente (admin nao pode excluir a si mesmo)")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UserDetails principal,
                                        @PathVariable Long id) {
        usuarioService.excluir(id, principal.getUsername());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
