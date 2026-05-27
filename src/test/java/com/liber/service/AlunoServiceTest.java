package com.liber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.liber.dto.AlunoRequest;
import com.liber.dto.AlunoResponse;
import com.liber.dto.auth.RegisterAlunoRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.Aluno;
import com.liber.entity.Role;
import com.liber.entity.Usuario;
import com.liber.exception.BusinessException;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.ReservaRepository;
import com.liber.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cobre o fluxo CPF do AlunoService: validacao de digito verificador, cadastro,
 * auto-registro do aluno (proteção anti-sequestro via nome) e geracao de email
 * sintetico baseado em CPF.
 */
@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    /** CPF valido conhecido com digito verificador correto. */
    private static final String CPF_VALIDO = "12345678909";
    private static final String CPF_VALIDO_FORMATADO = "123.456.789-09";

    @Mock AlunoRepository alunoRepository;
    @Mock EmprestimoRepository emprestimoRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AlunoService service;

    @BeforeEach
    void setUp() {
        service = new AlunoService(
            alunoRepository, emprestimoRepository, reservaRepository,
            usuarioRepository, passwordEncoder);
    }

    private static Aluno alunoExistente() {
        return Aluno.builder()
            .id(10L).cpf(CPF_VALIDO).nome("Ana Beatriz Silva").turma("9A")
            .build();
    }

    // ---------- cadastrar() ----------

    @Test
    void cadastrar_aceita_cpf_valido_e_normaliza() {
        AlunoRequest req = new AlunoRequest(CPF_VALIDO_FORMATADO, "Ana Beatriz", "9A");
        when(alunoRepository.existsByCpf(CPF_VALIDO)).thenReturn(false);
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(inv -> {
            Aluno a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        AlunoResponse resp = service.cadastrar(req);

        // Resposta vem com CPF formatado (pro bibliotecario)
        assertThat(resp.cpf()).isEqualTo(CPF_VALIDO_FORMATADO);
    }

    @Test
    void cadastrar_rejeita_cpf_com_digito_verificador_invalido() {
        // Mesmos digitos, ultimo trocado
        AlunoRequest req = new AlunoRequest("12345678900", "Ana", "9A");

        assertThatThrownBy(() -> service.cadastrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("CPF invalido");
    }

    @Test
    void cadastrar_rejeita_cpf_com_todos_digitos_iguais() {
        // 11111111111 passa no calculo do digito verificador mas e invalido
        AlunoRequest req = new AlunoRequest("11111111111", "Ana", "9A");

        assertThatThrownBy(() -> service.cadastrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("CPF invalido");
    }

    @Test
    void cadastrar_rejeita_cpf_ja_existente() {
        AlunoRequest req = new AlunoRequest(CPF_VALIDO, "Ana", "9A");
        when(alunoRepository.existsByCpf(CPF_VALIDO)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("CPF ja cadastrado");
    }

    // ---------- autoRegistrar() ----------

    @Test
    void autoRegistrar_cria_usuario_quando_dados_conferem() {
        RegisterAlunoRequest req = new RegisterAlunoRequest(
            CPF_VALIDO_FORMATADO, "Ana Beatriz Silva", "senha-segura-123");
        when(alunoRepository.findByCpf(CPF_VALIDO)).thenReturn(Optional.of(alunoExistente()));
        when(usuarioRepository.existsByAlunoId(10L)).thenReturn(false);
        when(passwordEncoder.encode("senha-segura-123")).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        UsuarioResponse resp = service.autoRegistrar(req);

        // Email sintetico inclui o CPF (so digitos) e dominio interno
        assertThat(resp.email()).isEqualTo("aluno." + CPF_VALIDO + "@liber.local");
        assertThat(resp.role()).isEqualTo(Role.ALUNO);
    }

    @Test
    void autoRegistrar_rejeita_nome_que_nao_bate() {
        RegisterAlunoRequest req = new RegisterAlunoRequest(
            CPF_VALIDO, "Carlos Mendes", "senha-segura-123");
        when(alunoRepository.findByCpf(CPF_VALIDO)).thenReturn(Optional.of(alunoExistente()));

        assertThatThrownBy(() -> service.autoRegistrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Dados nao conferem");
    }

    @Test
    void autoRegistrar_aceita_nome_com_diferenca_de_acentos_e_caixa() {
        // Cadastrado: "Ana Beatriz Silva". Digitado: "ANA BEATRIZ SÍLVA"
        // Sistema deve aceitar (normaliza acentos + lowercase).
        RegisterAlunoRequest req = new RegisterAlunoRequest(
            CPF_VALIDO, "ANA BEATRIZ SÍLVA", "senha-segura-123");
        when(alunoRepository.findByCpf(CPF_VALIDO)).thenReturn(Optional.of(alunoExistente()));
        when(usuarioRepository.existsByAlunoId(10L)).thenReturn(false);
        when(passwordEncoder.encode("senha-segura-123")).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.autoRegistrar(req)).isNotNull();
    }

    @Test
    void autoRegistrar_rejeita_cpf_inexistente_com_mensagem_neutra() {
        RegisterAlunoRequest req = new RegisterAlunoRequest(
            CPF_VALIDO, "Ana", "senha-segura-123");
        when(alunoRepository.findByCpf(CPF_VALIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autoRegistrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Dados nao conferem");
    }

    @Test
    void autoRegistrar_rejeita_cpf_invalido_com_mensagem_neutra() {
        // CPF invalido (digito verificador errado) — mensagem nao revela qual e o erro
        RegisterAlunoRequest req = new RegisterAlunoRequest(
            "12345678900", "Ana", "senha-segura-123");

        assertThatThrownBy(() -> service.autoRegistrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Dados nao conferem");
    }

    @Test
    void autoRegistrar_avisa_quando_ja_existe_acesso() {
        RegisterAlunoRequest req = new RegisterAlunoRequest(
            CPF_VALIDO_FORMATADO, "Ana Beatriz Silva", "senha-segura-123");
        when(alunoRepository.findByCpf(CPF_VALIDO)).thenReturn(Optional.of(alunoExistente()));
        when(usuarioRepository.existsByAlunoId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.autoRegistrar(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ja tem cadastro");
    }
}
