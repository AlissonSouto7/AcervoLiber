package com.liber.service;

import com.liber.dto.auth.LoginAlunoRequest;
import com.liber.dto.auth.LoginRequest;
import com.liber.dto.auth.LoginResponse;
import com.liber.dto.auth.RegisterRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.Role;
import com.liber.entity.Usuario;
import com.liber.exception.ContaBloqueadaException;
import com.liber.repository.UsuarioRepository;
import com.liber.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    // Injetado como proxy request-scoped pelo Spring — resolve para o request atual.
    // Usado para extrair o IP do cliente no lockout por (email + IP), evitando
    // poisoning (atacante de um IP nao bloqueia a conta da vitima em outro IP).
    private final HttpServletRequest httpRequest;

    /**
     * Hash BCrypt valido pra rodar match dummy quando a matricula nao existe.
     * Custa o mesmo tempo de um hash real e nao permite que o tempo de resposta
     * diferencie "matricula existe, senha errada" de "matricula nao existe".
     * Sem isto, atacante mede tempo e enumera matriculas de menores (LGPD).
     */
    private static final String DUMMY_HASH =
        "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /** Login da equipe (ADMIN/BIBLIOTECARIO) — por e-mail. */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        return autenticar(req.email().trim().toLowerCase(), req.senha());
    }

    /** Login do aluno — por matricula. Resolve a matricula para o usuario vinculado. */
    @Transactional
    public LoginResponse loginAluno(LoginAlunoRequest req) {
        String matricula = req.matricula().trim();
        Usuario usuario = usuarioRepository.findByAlunoMatricula(matricula)
            .filter(u -> u.getRole() == Role.ALUNO)
            .orElse(null);
        if (usuario == null) {
            // Equaliza o tempo de resposta: roda BCrypt contra um hash dummy de
            // modo que matricula inexistente custe ~ o mesmo que matricula com
            // senha errada. Sem isto, atacante mede tempo e enumera matriculas.
            passwordEncoder.matches(req.senha(), DUMMY_HASH);
            log.warn("Login falhou: matricula inexistente {} (IP={})", matricula, clientIp());
            throw new BadCredentialsException("Matricula ou senha incorretos");
        }
        return autenticar(usuario.getEmail(), req.senha());
    }

    /** Fluxo comum de autenticacao — bloqueio por conta+IP, auditoria e emissao de tokens. */
    private LoginResponse autenticar(String email, String senhaRaw) {
        // Lockout e por (email + IP): impede um atacante de um IP de bloquear a
        // conta da vitima em outro IP (poisoning). Brute-force de um unico IP
        // ainda e bloqueado normalmente.
        String ip = clientIp();
        try {
            loginAttemptService.verificarBloqueio(email, ip);
        } catch (ContaBloqueadaException e) {
            log.warn("Login bloqueado por tentativas: email={} IP={}", email, ip);
            throw e;
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senhaRaw)
            );
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFalha(email, ip);
            log.warn("Login falhou: credenciais invalidas email={} IP={}", email, ip);
            throw e;
        }
        loginAttemptService.registrarSucesso(email, ip);

        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Usuario autenticado mas nao encontrado: " + email));

        log.info("Login bem-sucedido email={}", email);
        return emitirTokens(usuario);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        RefreshTokenService.Rotacao rotacao = refreshTokenService.rotacionar(refreshToken);
        Usuario usuario = rotacao.usuario();

        String accessToken = jwtService.generateToken(toUserDetails(usuario));
        log.info("Access token renovado para usuario id={}", usuario.getId());

        return LoginResponse.of(accessToken, rotacao.novoRefreshToken(),
            jwtService.getExpirationMs(), UsuarioResponse.from(usuario));
    }

    @Transactional
    public void logout(String refreshToken, String principalEmail) {
        refreshTokenService.revogarSeDoUsuario(refreshToken, principalEmail);
        // Bumpa passwordChangedAt para invalidar access tokens existentes deste usuario
        // imediatamente — sem isso, o access vivia ate exp (~15min) apos "Sair",
        // dando janela ofensiva pra sessao roubada. Filtro JWT rejeita tokens com
        // iat < passwordChangedAt. Mesmo padrao de alterarStatus(false)/alterarSenha.
        usuarioRepository.findByEmail(principalEmail).ifPresent(u -> {
            u.setPasswordChangedAt(Instant.now());
            usuarioRepository.save(u);
        });
        log.info("Logout email={} (refresh token revogado)", principalEmail);
    }

    @Transactional
    public UsuarioResponse register(RegisterRequest req) {
        // Por padrao cria como BIBLIOTECARIO. ADMIN deve ser promovido via seed/endpoint admin.
        return usuarioService.registrar(req, Role.BIBLIOTECARIO);
    }

    private LoginResponse emitirTokens(Usuario usuario) {
        String accessToken = jwtService.generateToken(toUserDetails(usuario));
        String refreshToken = refreshTokenService.gerar(usuario);
        return LoginResponse.of(accessToken, refreshToken,
            jwtService.getExpirationMs(), UsuarioResponse.from(usuario));
    }

    private UserDetails toUserDetails(Usuario usuario) {
        return User.withUsername(usuario.getEmail())
            .password(usuario.getSenhaHash())
            .authorities(usuario.getRole().authority())
            .build();
    }

    /** IP do cliente — confiavel apos `server.forward-headers-strategy=native` configurar trust-list. */
    private String clientIp() {
        String addr = httpRequest.getRemoteAddr();
        return addr == null ? "unknown" : addr;
    }
}
