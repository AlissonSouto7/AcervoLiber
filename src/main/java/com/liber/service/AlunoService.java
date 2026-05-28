package com.liber.service;

import com.liber.dto.AlunoRequest;
import com.liber.dto.AlunoResponse;
import com.liber.dto.auth.RegisterAlunoRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Role;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.exception.ResourceNotFoundException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.ReservaRepository;
import com.liber.repository.UsuarioRepository;
import com.liber.util.Cpf;
import java.text.Normalizer;
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

    public Page<AlunoResponse> listar(String termo, Pageable pageable) {
        // Normaliza termo de busca: se for CPF formatado (123.456.789-01), busca
        // por digitos puros (porque no banco gravamos so digitos).
        String termoNorm = normalizarTermoBusca(termo);
        return alunoRepository.buscar(termoNorm, pageable)
            .map(a -> AlunoResponse.from(a, contarEmprestimosAtivos(a.getId())));
    }

    public AlunoResponse buscarPorId(Long id) {
        Aluno aluno = carregar(id);
        return AlunoResponse.from(aluno, contarEmprestimosAtivos(id));
    }

    @Transactional
    public AlunoResponse cadastrar(AlunoRequest req) {
        String cpf = validarENormalizarCpf(req.cpf());
        if (alunoRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF ja cadastrado.");
        }

        Aluno aluno = Aluno.builder()
            .cpf(cpf)
            .nome(req.nome().trim())
            .turma(req.turma().trim())
            .build();

        Aluno salvo = alunoRepository.save(aluno);
        log.info("Aluno cadastrado id={} cpf={}", salvo.getId(), Cpf.mask(salvo.getCpf()));
        return AlunoResponse.from(salvo, 0L);
    }

    @Transactional
    public AlunoResponse atualizar(Long id, AlunoRequest req) {
        Aluno aluno = carregar(id);

        String cpfNovo = validarENormalizarCpf(req.cpf());
        if (!cpfNovo.equals(aluno.getCpf()) && alunoRepository.existsByCpf(cpfNovo)) {
            throw new BusinessException("CPF ja cadastrado.");
        }

        aluno.setCpf(cpfNovo);
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
     * Auto-cadastro do aluno na tela publica de login.
     *
     * <p>Pra evitar sequestro de conta exigimos que:
     * <ul>
     *   <li>CPF existe (aluno foi pre-cadastrado pela escola)</li>
     *   <li>nome do request bate com cadastro (normalizado: sem acentos, lowercase)</li>
     *   <li>aluno ainda nao tem Usuario vinculado</li>
     * </ul>
     */
    @Transactional
    public UsuarioResponse autoRegistrar(RegisterAlunoRequest req) {
        String cpf;
        try {
            cpf = validarENormalizarCpf(req.cpf());
        } catch (BusinessException e) {
            throw new BusinessException("Dados nao conferem. Verifique nome e CPF com a escola.");
        }

        Aluno aluno = alunoRepository.findByCpf(cpf).orElse(null);
        if (aluno == null) {
            throw new BusinessException("Dados nao conferem. Verifique nome e CPF com a escola.");
        }

        if (!nomesIguais(req.nome(), aluno.getNome())) {
            throw new BusinessException("Dados nao conferem. Verifique nome e CPF com a escola.");
        }

        if (usuarioRepository.existsByAlunoId(aluno.getId())) {
            throw new BusinessException("Voce ja tem cadastro. Faca login normalmente.");
        }

        // Bloqueia senha contendo o proprio nome ou CPF do aluno
        validarSenhaAluno(req.senha(), aluno.getNome(), cpf);

        String email = "aluno." + cpf + "@liber.local";
        Usuario usuario = Usuario.builder()
            .email(email)
            .nome(aluno.getNome())
            .senhaHash(passwordEncoder.encode(req.senha()))
            .role(Role.ALUNO)
            .ativo(true)
            .passwordChangedAt(Instant.now())
            .deveTrocarSenha(false)
            .aluno(aluno)
            .build();

        Usuario salvo;
        try {
            salvo = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Voce ja tem cadastro. Faca login normalmente.");
        }
        log.info("Auto-cadastro de aluno cpf={} email={}", Cpf.mask(aluno.getCpf()), salvo.getEmail());
        return UsuarioResponse.from(salvo);
    }

    private static boolean nomesIguais(String a, String b) {
        return normalizarNome(a).equals(normalizarNome(b));
    }

    private static String normalizarNome(String nome) {
        if (nome == null) return "";
        String norm = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return norm.toLowerCase().replaceAll("\\s+", " ");
    }

    /** Valida CPF (digito verificador) e retorna a versao normalizada (so digitos). */
    private static String validarENormalizarCpf(String cpf) {
        String norm = Cpf.normalize(cpf);
        if (norm == null || !Cpf.isValid(norm)) {
            throw new BusinessException("CPF invalido.");
        }
        return norm;
    }

    /**
     * Termo de busca: se for CPF formatado (com pontos/hifen), normaliza pra
     * digitos puros — o banco grava so digitos, entao "123.456.789-01" no input
     * precisa virar "12345678901" pra match LIKE funcionar.
     */
    private static String normalizarTermoBusca(String termo) {
        if (termo == null || termo.isBlank()) return termo;
        // Se contem ponto ou hifen e o resto sao digitos, e provavel um CPF formatado
        if (termo.matches("^[0-9.\\-]+$")) {
            return termo.replaceAll("\\D", "");
        }
        return termo;
    }

    /**
     * Cria o acesso de login de um aluno (portal do aluno). Senha provisoria,
     * obrigatoria troca no primeiro acesso.
     */
    @Transactional
    public UsuarioResponse criarAcesso(Long alunoId, String senhaInicial) {
        Aluno aluno = carregar(alunoId);
        if (usuarioRepository.existsByAlunoId(alunoId)) {
            throw new BusinessException("Este aluno ja possui acesso ao sistema.");
        }

        String email = "aluno." + aluno.getCpf() + "@liber.local";

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
            throw new BusinessException(
                "Este aluno ja possui acesso (criado por outro usuario simultaneamente).");
        }
        log.info("Acesso de aluno criado para cpf={} email={}",
            Cpf.mask(aluno.getCpf()), salvo.getEmail());
        return UsuarioResponse.from(salvo);
    }

    /** Bloqueia senha que contem nome ou CPF do proprio aluno. */
    private static void validarSenhaAluno(String senha, String nome, String cpf) {
        if (senha == null) return;
        String s = java.text.Normalizer.normalize(senha.trim(), java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
        if (nome != null) {
            String n = java.text.Normalizer.normalize(nome.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
            for (String parte : n.split("\\s+")) {
                if (parte.length() >= 4 && s.contains(parte)) {
                    throw new BusinessException("A senha nao pode conter partes do seu nome.");
                }
            }
        }
        if (cpf != null && cpf.length() >= 6 && s.contains(cpf)) {
            throw new BusinessException("A senha nao pode conter o seu CPF.");
        }
    }

    Aluno carregar(Long id) {
        return alunoRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Aluno", id));
    }

    long contarEmprestimosAtivos(Long alunoId) {
        return emprestimoRepository.countByAlunoIdAndSituacao(alunoId, SituacaoEmprestimo.ATIVO);
    }
}
