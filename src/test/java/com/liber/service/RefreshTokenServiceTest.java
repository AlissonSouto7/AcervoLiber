package com.liber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liber.config.JwtProperties;
import com.liber.entity.RefreshToken;
import com.liber.entity.Role;
import com.liber.entity.Usuario;
import com.liber.exception.RefreshTokenInvalidoException;
import com.liber.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long REFRESH_MS = 604_800_000L; // 7 dias
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock RefreshTokenRepository repository;

    private final JwtProperties props =
        new JwtProperties("test-secret-com-32-bytes-no-minimo-aaaa", 900_000L, REFRESH_MS, "test");
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, props, CLOCK);
    }

    private static Usuario usuario() {
        return Usuario.builder()
            .id(1L).email("u@test.com").nome("U")
            .senhaHash("x").role(Role.BIBLIOTECARIO).ativo(true)
            .passwordChangedAt(Instant.now(CLOCK))
            .build();
    }

    @Test
    void gerar_persiste_o_hash_e_retorna_o_valor_puro() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.gerar(usuario());

        assertThat(raw).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken salvo = captor.getValue();

        // Nunca persiste o texto puro — apenas o hash SHA-256 (64 chars hex)
        assertThat(salvo.getTokenHash()).isNotEqualTo(raw).hasSize(64);
        assertThat(salvo.getExpiresAt()).isEqualTo(Instant.now(CLOCK).plusMillis(REFRESH_MS));
    }

    @Test
    void rotacionar_token_valido_revoga_o_antigo_e_emite_um_novo() {
        RefreshToken atual = RefreshToken.builder()
            .id(10L).usuario(usuario())
            .tokenHash(RefreshTokenService.hash("raw-token"))
            .expiresAt(Instant.now(CLOCK).plusSeconds(3600))
            .build();
        when(repository.findByTokenHash(RefreshTokenService.hash("raw-token")))
            .thenReturn(Optional.of(atual));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.Rotacao result = service.rotacionar("raw-token");

        assertThat(atual.isRevogado()).isTrue();
        assertThat(result.novoRefreshToken()).isNotBlank().isNotEqualTo("raw-token");
        assertThat(result.usuario().getId()).isEqualTo(1L);
    }

    @Test
    void rotacionar_token_inexistente_lanca_excecao() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotacionar("nao-existe"))
            .isInstanceOf(RefreshTokenInvalidoException.class);
    }

    @Test
    void rotacionar_token_expirado_lanca_excecao() {
        RefreshToken expirado = RefreshToken.builder()
            .id(10L).usuario(usuario())
            .tokenHash(RefreshTokenService.hash("raw"))
            .expiresAt(Instant.now(CLOCK).minusSeconds(1))
            .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> service.rotacionar("raw"))
            .isInstanceOf(RefreshTokenInvalidoException.class)
            .hasMessageContaining("expirado");
    }

    @Test
    void rotacionar_token_revogado_detecta_reuso_e_revoga_a_familia_inteira() {
        RefreshToken revogado = RefreshToken.builder()
            .id(10L).usuario(usuario())
            .tokenHash(RefreshTokenService.hash("raw"))
            .expiresAt(Instant.now(CLOCK).plusSeconds(3600))
            .revokedAt(Instant.now(CLOCK).minusSeconds(10))
            .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(revogado));

        assertThatThrownBy(() -> service.rotacionar("raw"))
            .isInstanceOf(RefreshTokenInvalidoException.class)
            .hasMessageContaining("reutilizado");

        verify(repository).revogarTodosDoUsuario(eq(1L), any());
    }

    @Test
    void revogarSeDoUsuario_com_dono_correto_marca_o_token_como_revogado() {
        RefreshToken ativo = RefreshToken.builder()
            .id(10L).usuario(usuario())
            .tokenHash(RefreshTokenService.hash("raw"))
            .expiresAt(Instant.now(CLOCK).plusSeconds(3600))
            .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(ativo));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.revogarSeDoUsuario("raw", "u@test.com");

        assertThat(ativo.isRevogado()).isTrue();
    }

    @Test
    void revogarSeDoUsuario_com_dono_errado_nao_revoga() {
        RefreshToken ativo = RefreshToken.builder()
            .id(10L).usuario(usuario())
            .tokenHash(RefreshTokenService.hash("raw"))
            .expiresAt(Instant.now(CLOCK).plusSeconds(3600))
            .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(ativo));

        // Atacante tenta revogar token de outro usuario — ignora silenciosamente
        service.revogarSeDoUsuario("raw", "atacante@evil.com");

        assertThat(ativo.isRevogado()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void revogarSeDoUsuario_token_inexistente_nao_falha() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revogarSeDoUsuario("nao-existe", "u@test.com");

        verify(repository, never()).save(any());
    }

    @Test
    void revogarTodosDoUsuario_propaga_para_o_repository() {
        when(repository.revogarTodosDoUsuario(eq(42L), any())).thenReturn(3);

        int revogados = service.revogarTodosDoUsuario(42L);

        assertThat(revogados).isEqualTo(3);
        verify(repository).revogarTodosDoUsuario(eq(42L), any());
    }
}
