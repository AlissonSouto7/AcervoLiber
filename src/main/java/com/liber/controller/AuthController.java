package com.liber.controller;

import com.liber.config.AuthProperties;
import com.liber.dto.auth.AtualizarPerfilRequest;
import com.liber.dto.auth.ChangePasswordRequest;
import com.liber.dto.auth.LoginAlunoRequest;
import com.liber.dto.auth.LoginRequest;
import com.liber.dto.auth.LoginResponse;
import com.liber.dto.auth.RefreshTokenRequest;
import com.liber.dto.auth.RegisterAlunoRequest;
import com.liber.dto.auth.RegisterRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.exception.RegistroPublicoDesabilitadoException;
import com.liber.service.AlunoService;
import com.liber.service.AuthService;
import com.liber.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticacao e registro de usuarios")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final AlunoService alunoService;
    private final AuthProperties authProperties;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica e retorna access token (JWT) + refresh token")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/login-aluno")
    @SecurityRequirements
    @Operation(summary = "Autentica um aluno por matricula e senha")
    public LoginResponse loginAluno(@Valid @RequestBody LoginAlunoRequest req) {
        return authService.loginAluno(req);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renova o access token usando um refresh token valido (rotaciona o refresh)")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoga o refresh token informado (logout). Exige autenticacao — o token deve pertencer ao usuario logado.")
    public void logout(@AuthenticationPrincipal UserDetails principal,
                       @Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.refreshToken(), principal.getUsername());
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Registro publico (cria BIBLIOTECARIO). Pode estar desabilitado via app.auth.public-register-enabled=false")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (!authProperties.publicRegisterEnabled()) {
            throw new RegistroPublicoDesabilitadoException();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/register-aluno")
    @SecurityRequirements
    @Operation(summary = "Auto-cadastro de aluno (matricula+nome+senha). Exige que o aluno tenha sido pre-cadastrado pelo bibliotecario.")
    public ResponseEntity<UsuarioResponse> registerAluno(@Valid @RequestBody RegisterAlunoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alunoService.autoRegistrar(req));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuario autenticado")
    public UsuarioResponse me(@AuthenticationPrincipal UserDetails principal) {
        return usuarioService.buscarPorEmail(principal.getUsername());
    }

    @PutMapping("/perfil")
    @Operation(summary = "Atualiza os dados do proprio usuario (nome)")
    public UsuarioResponse atualizarPerfil(@AuthenticationPrincipal UserDetails principal,
                                            @Valid @RequestBody AtualizarPerfilRequest req) {
        return usuarioService.atualizarPerfil(principal.getUsername(), req.nome());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Altera a senha do usuario autenticado (exige senha atual)")
    public void changePassword(@AuthenticationPrincipal UserDetails principal,
                                @Valid @RequestBody ChangePasswordRequest req) {
        usuarioService.alterarSenha(principal.getUsername(), req.senhaAtual(), req.senhaNova());
    }
}
