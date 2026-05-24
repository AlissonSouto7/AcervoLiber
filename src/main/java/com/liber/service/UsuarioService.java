package com.liber.service;

import com.liber.dto.auth.CriarUsuarioRequest;
import com.liber.dto.auth.RegisterRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.Role;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.UsuarioRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UsuarioResponse buscarPorEmail(String email) {
        Usuario u = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + email));
        return UsuarioResponse.from(u);
    }

    @Transactional
    public UsuarioResponse registrar(RegisterRequest req, Role role) {
        return criarInterno(req.email(), req.nome(), req.senha(), role);
    }

    @Transactional
    public UsuarioResponse criarComoAdmin(CriarUsuarioRequest req) {
        // Role ALUNO NUNCA pode nascer por este caminho — quebraria o invariante
        // "Usuario com role ALUNO esta vinculado a um Aluno (matricula)". Contas
        // de aluno so podem ser criadas via POST /api/v1/alunos/{id}/acesso.
        if (req.role() == Role.ALUNO) {
            throw new BusinessException(
                "Para criar acesso de aluno, use POST /api/v1/alunos/{id}/acesso");
        }
        return criarInterno(req.email(), req.nome(), req.senha(), req.role());
    }

    private UsuarioResponse criarInterno(String emailBruto, String nome, String senha, Role role) {
        String email = emailBruto.trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("Email ja cadastrado: " + email);
        }

        Usuario novo = Usuario.builder()
            .email(email)
            .nome(nome.trim())
            .senhaHash(passwordEncoder.encode(senha))
            .role(role)
            .ativo(true)
            .passwordChangedAt(Instant.now())
            .build();

        Usuario salvo = usuarioRepository.save(novo);
        log.info("Usuario criado id={} email={} role={}", salvo.getId(), salvo.getEmail(), salvo.getRole());
        return UsuarioResponse.from(salvo);
    }

    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::from);
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(String email, String nome) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + email));
        // Aluno NAO pode mudar nome — e o nome oficial cadastrado pela escola.
        // Bibliotecario edita via tela de Alunos quando precisa corrigir.
        // Sem este bloqueio, o aluno mudava Usuario.nome mas Aluno.nome (o que aparece
        // na tela do bibliotecario) ficava intocado, causando dessincronia confusa.
        if (usuario.getRole() == Role.ALUNO) {
            throw new BusinessException(
                "Alunos nao podem alterar o nome. Procure o bibliotecario(a) se houver erro de cadastro.");
        }
        usuario.setNome(nome.trim());
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Perfil atualizado para usuario id={}", usuario.getId());
        return UsuarioResponse.from(salvo);
    }

    @Transactional
    public UsuarioResponse alterarStatus(Long id, boolean ativo, String principalEmail) {
        Usuario u = usuarioRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));

        // Defesa em profundidade contra self-lockout do administrador:
        if (!ativo) {
            // (1) Admin nao pode desativar a si mesmo (clique acidental = perde
            //     acesso imediato; pode forcar abertura de outro admin no banco).
            if (principalEmail != null && principalEmail.equalsIgnoreCase(u.getEmail())) {
                throw new BusinessException(
                    "Voce nao pode desativar a si mesmo. Peca a outro administrador.");
            }
            // (2) Nunca deixar o sistema sem nenhum ADMIN ativo — se o alvo for
            //     ADMIN, conferir se sobra pelo menos um outro ADMIN ativo.
            if (u.getRole() == Role.ADMIN
                    && usuarioRepository.countByRoleAndAtivoIsTrueAndIdNot(Role.ADMIN, id) == 0) {
                throw new BusinessException(
                    "Nao e possivel desativar o ultimo administrador ativo. "
                    + "Crie ou ative outro admin antes.");
            }
        }

        u.setAtivo(ativo);
        // Ao DESATIVAR: bumpa passwordChangedAt para invalidar access tokens existentes
        // imediatamente (filtro JWT rejeita tokens com iat < passwordChangedAt) E revoga
        // todos os refresh tokens — encerra todas as sessoes do usuario na hora.
        // Sem isso, um usuario "desativado" continua emitindo tokens via /auth/refresh
        // por ate 7 dias, ou usando o access token atual por ate 15 min.
        if (!ativo) {
            u.setPasswordChangedAt(Instant.now());
        }
        Usuario salvo = usuarioRepository.save(u);
        if (!ativo) {
            refreshTokenService.revogarTodosDoUsuario(id);
        }
        log.info("Status do usuario id={} alterado para ativo={}", id, ativo);
        return UsuarioResponse.from(salvo);
    }

    @Transactional
    public void alterarSenha(String email, String senhaAtual, String senhaNova) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + email));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new BadCredentialsException("Senha atual incorreta");
        }

        if (passwordEncoder.matches(senhaNova, usuario.getSenhaHash())) {
            throw new BusinessException("A nova senha deve ser diferente da atual");
        }

        usuario.setSenhaHash(passwordEncoder.encode(senhaNova));
        usuario.setPasswordChangedAt(Instant.now());
        usuario.setDeveTrocarSenha(false);
        usuarioRepository.save(usuario);
        // Revoga todos os refresh tokens do usuario — uma sessao atacante (com refresh
        // roubado, ex.: XSS) nao sobrevive a troca de senha. O proprio usuario sera
        // pedido para logar de novo no proximo refresh — esperado.
        refreshTokenService.revogarTodosDoUsuario(usuario.getId());
        log.info("Senha alterada para usuario id={} email={}", usuario.getId(), usuario.getEmail());
    }
}
