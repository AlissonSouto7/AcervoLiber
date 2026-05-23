package com.liber.service;

import com.liber.dto.AlunoRequest;
import com.liber.dto.AlunoResponse;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.Aluno;
import com.liber.entity.EventoAuditoria;
import com.liber.entity.Role;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.ReservaRepository;
import com.liber.repository.UsuarioRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public Page<AlunoResponse> listar(String termo, Pageable pageable) {
        return alunoRepository.buscar(termo, pageable)
            .map(a -> AlunoResponse.from(a, contarEmprestimosAtivos(a.getId())));
    }

    public AlunoResponse buscarPorId(Long id) {
        Aluno aluno = carregar(id);
        return AlunoResponse.from(aluno, contarEmprestimosAtivos(id));
    }

    @Transactional
    public AlunoResponse cadastrar(AlunoRequest req) {
        String matricula = req.matricula().trim();
        if (alunoRepository.existsByMatricula(matricula)) {
            throw new BusinessException("Matricula ja cadastrada: " + matricula);
        }

        Aluno aluno = Aluno.builder()
            .matricula(matricula)
            .nome(req.nome().trim())
            .turma(req.turma().trim())
            .build();

        Aluno salvo = alunoRepository.save(aluno);
        log.info("Aluno cadastrado id={} matricula={}", salvo.getId(), salvo.getMatricula());
        return AlunoResponse.from(salvo, 0L);
    }

    @Transactional
    public AlunoResponse atualizar(Long id, AlunoRequest req) {
        Aluno aluno = carregar(id);

        String matriculaNova = req.matricula().trim();
        if (!matriculaNova.equals(aluno.getMatricula())
                && alunoRepository.existsByMatricula(matriculaNova)) {
            throw new BusinessException("Matricula ja cadastrada: " + matriculaNova);
        }

        aluno.setMatricula(matriculaNova);
        aluno.setNome(req.nome().trim());
        aluno.setTurma(req.turma().trim());

        log.info("Aluno atualizado id={}", id);
        Aluno salvo = alunoRepository.save(aluno);
        return AlunoResponse.from(salvo, contarEmprestimosAtivos(id));
    }

    @Transactional
    public void remover(Long id) {
        if (!alunoRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Aluno", id);
        }
        // Pre-checagens defensivas com mensagens amigaveis — sem isso, a remocao
        // viola FK e o GlobalExceptionHandler retorna 409 "Conflito de dados" generico
        // que o bibliotecario nao consegue diagnosticar.
        if (emprestimoRepository.existsByAlunoId(id)) {
            throw new BusinessException(
                "Nao e possivel remover aluno com historico de emprestimos. Mantenha-o para preservar o historico.");
        }
        if (reservaRepository.existsByAlunoId(id)) {
            throw new BusinessException(
                "Aluno possui reservas no historico. Cancele as pendentes antes de remover.");
        }
        if (usuarioRepository.existsByAlunoId(id)) {
            throw new BusinessException(
                "Aluno possui acesso ao sistema (login do portal). Remova o acesso antes.");
        }
        alunoRepository.deleteById(id);
        log.info("Aluno removido id={}", id);
    }

    /**
     * Cria o acesso de login de um aluno (portal do aluno). Gera um Usuario com
     * role ALUNO vinculado ao registro Aluno, com senha provisoria — o aluno e
     * obrigado a troca-la no primeiro acesso.
     */
    @Transactional
    public UsuarioResponse criarAcesso(Long alunoId, String senhaInicial) {
        Aluno aluno = carregar(alunoId);
        if (usuarioRepository.existsByAlunoId(alunoId)) {
            throw new BusinessException("Este aluno ja possui acesso ao sistema.");
        }

        // Email sintetico — o aluno loga por matricula; este valor satisfaz a
        // unicidade/formato do campo e nao e usado para login.
        String email = "aluno." + aluno.getMatricula().trim().toLowerCase() + "@liber.local";

        Usuario usuario = Usuario.builder()
            .email(email)
            .nome(aluno.getNome())
            .senhaHash(passwordEncoder.encode(senhaInicial))
            .role(Role.ALUNO)
            .ativo(true)
            .passwordChangedAt(Instant.now())
            .deveTrocarSenha(true)
            .aluno(aluno)
            .build();

        Usuario salvo;
        try {
            salvo = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Race com outro bibliotecario criando acesso para o mesmo aluno
            // (unique constraint em usuarios.aluno_id ou usuarios.email pega).
            // Sem este catch, vira 409 "Conflito de dados" generico.
            throw new BusinessException(
                "Este aluno ja possui acesso (criado por outro usuario simultaneamente).");
        }
        auditService.registrar(EventoAuditoria.USUARIO_CRIADO, salvo.getEmail(),
            "Acesso de aluno (matricula " + aluno.getMatricula() + ")");
        log.info("Acesso de aluno criado para matricula={}", aluno.getMatricula());
        return UsuarioResponse.from(salvo);
    }

    Aluno carregar(Long id) {
        return alunoRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Aluno", id));
    }

    long contarEmprestimosAtivos(Long alunoId) {
        return emprestimoRepository.countByAlunoIdAndSituacao(alunoId, SituacaoEmprestimo.ATIVO);
    }
}
