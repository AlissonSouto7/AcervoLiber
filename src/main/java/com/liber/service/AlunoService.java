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
     * Auto-cadastro do aluno na tela publica de login.
     *
     * <p>Diferente de {@link #criarAcesso} (chamado pelo bibliotecario), aqui o
     * proprio aluno escolhe a senha. Pra evitar sequestro de conta (atacante que
     * sabe a matricula do colega), exigimos que:
     * <ul>
     *   <li>matricula existe (aluno foi pre-cadastrado pela escola)</li>
     *   <li>nome do request bate com cadastro (normalizado: case-insensitive, sem acentos)</li>
     *   <li>aluno ainda nao tem Usuario vinculado</li>
     * </ul>
     *
     * <p>Mensagem de erro neutra ("dados nao conferem") em caso de matricula
     * inexistente OU nome divergente — nao revela se a matricula foi cadastrada.
     */
    @Transactional
    public UsuarioResponse autoRegistrar(RegisterAlunoRequest req) {
        String matricula = req.matricula() == null ? "" : req.matricula().trim();
        if (matricula.isEmpty()) {
            throw new BusinessException("Matricula obrigatoria.");
        }

        Aluno aluno = alunoRepository.findByMatricula(matricula).orElse(null);
        if (aluno == null) {
            // Mensagem neutra — nao confirma/nega existencia da matricula.
            throw new BusinessException("Dados nao conferem. Verifique nome e matricula com a escola.");
        }

        // Validacao de nome — defesa contra sequestro: atacante que so saiba a
        // matricula precisa tambem saber o nome completo cadastrado.
        if (!nomesIguais(req.nome(), aluno.getNome())) {
            throw new BusinessException("Dados nao conferem. Verifique nome e matricula com a escola.");
        }

        if (usuarioRepository.existsByAlunoId(aluno.getId())) {
            // Aqui podemos ser explicitos — o aluno legitimo precisa saber pra logar.
            throw new BusinessException("Voce ja tem cadastro. Faca login normalmente.");
        }

        String email = "aluno." + matricula.toLowerCase() + "@liber.local";
        Usuario usuario = Usuario.builder()
            .email(email)
            .nome(aluno.getNome())              // usa o nome CADASTRADO (case original), nao o do request
            .senhaHash(passwordEncoder.encode(req.senha()))
            .role(Role.ALUNO)
            .ativo(true)
            .passwordChangedAt(Instant.now())
            .deveTrocarSenha(false)              // ja definiu senha no auto-cadastro
            .aluno(aluno)
            .build();

        Usuario salvo;
        try {
            salvo = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Race: outro request auto-cadastrando o mesmo aluno no mesmo segundo.
            throw new BusinessException("Voce ja tem cadastro. Faca login normalmente.");
        }
        log.info("Auto-cadastro de aluno matricula={} email={}", aluno.getMatricula(), salvo.getEmail());
        return UsuarioResponse.from(salvo);
    }

    /**
     * Normaliza e compara dois nomes: trim, multiplas espacos -> 1, lowercase,
     * remove acentos. Permite que "Joao da Silva" == " JOÃO  DA SILVA " == "joão da silva".
     */
    private static boolean nomesIguais(String a, String b) {
        return normalizarNome(a).equals(normalizarNome(b));
    }

    private static String normalizarNome(String nome) {
        if (nome == null) return "";
        String norm = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return norm.toLowerCase().replaceAll("\\s+", " ");
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
        log.info("Acesso de aluno criado para matricula={} email={}", aluno.getMatricula(), salvo.getEmail());
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
