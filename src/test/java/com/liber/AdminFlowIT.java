package com.liber;

import static org.assertj.core.api.Assertions.assertThat;

import com.liber.dto.auth.AtualizarStatusUsuarioRequest;
import com.liber.dto.auth.CriarUsuarioRequest;
import com.liber.dto.auth.LoginRequest;
import com.liber.dto.auth.LoginResponse;
import com.liber.dto.auth.RegisterRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.Role;
import com.liber.entity.Usuario;
import com.liber.repository.UsuarioRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

/**
 * Cobre os fluxos administrativos:
 *  - lockdown do /auth/register quando publicRegisterEnabled=false
 *  - POST /api/v1/usuarios so para ADMIN
 *  - PATCH /api/v1/usuarios/{id}/status ativando/desativando
 *  - JWT de usuario desativado deixa de funcionar
 *  - JWT antigo deixa de valer apos troca de senha
 */
@TestPropertySource(properties = "app.auth.public-register-enabled=false")
class AdminFlowIT extends AbstractIntegrationTest {

    @LocalServerPort int port;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private RestClient http;

    @BeforeEach
    void initClient() {
        http = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> { })
            .build();
    }

    @Test
    void register_publico_e_bloqueado_com_403_quando_desabilitado() {
        ResponseEntity<ProblemDetail> resp = post("/api/v1/auth/register",
            new RegisterRequest("qualquer@test.com", "Nome", "Tr0pical#T1"),
            null, ProblemDetail.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getTitle()).isEqualTo("Registro desabilitado");
    }

    @Test
    void admin_pode_criar_outros_usuarios_e_bibliotecario_nao() {
        // Cria admin diretamente no DB
        Usuario admin = criarUsuarioDireto("admin-test@test.com", "Tr0pical#A1", Role.ADMIN);
        String tokenAdmin = login(admin.getEmail(), "Tr0pical#A1");

        // Admin cria bibliotecario
        ResponseEntity<UsuarioResponse> criou = post("/api/v1/usuarios",
            new CriarUsuarioRequest("biblio@test.com", "Biblio", "PassStrong1@x", Role.BIBLIOTECARIO),
            tokenAdmin, UsuarioResponse.class);
        assertThat(criou.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criou.getBody().role()).isEqualTo(Role.BIBLIOTECARIO);

        // Bibliotecario nao pode acessar o endpoint admin
        String tokenBiblio = login("biblio@test.com", "PassStrong1@x");
        ResponseEntity<ProblemDetail> negado = post("/api/v1/usuarios",
            new CriarUsuarioRequest("outro@test.com", "Outro", "PassStrong2@x", Role.BIBLIOTECARIO),
            tokenBiblio, ProblemDetail.class);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void desativar_usuario_invalida_token_existente() {
        Usuario admin = criarUsuarioDireto("admin2@test.com", "Tr0pical#A2", Role.ADMIN);
        String tokenAdmin = login(admin.getEmail(), "Tr0pical#A2");

        UsuarioResponse vitima = post("/api/v1/usuarios",
            new CriarUsuarioRequest("vitima@test.com", "Vitima", "PassStrong3@x", Role.BIBLIOTECARIO),
            tokenAdmin, UsuarioResponse.class).getBody();

        String tokenVitima = login(vitima.email(), "PassStrong3@x");
        // Token funciona inicialmente
        assertThat(get("/api/v1/auth/me", tokenVitima).getStatusCode()).isEqualTo(HttpStatus.OK);

        // Admin desativa
        ResponseEntity<UsuarioResponse> desativou = http.patch()
            .uri("/api/v1/usuarios/" + vitima.id() + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> h.setBearerAuth(tokenAdmin))
            .body(new AtualizarStatusUsuarioRequest(false))
            .retrieve().toEntity(UsuarioResponse.class);
        assertThat(desativou.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(desativou.getBody().ativo()).isFalse();

        // Token da vitima deixa de funcionar
        assertThat(get("/api/v1/auth/me", tokenVitima).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void jwt_emitido_antes_da_troca_de_senha_e_invalidado() throws Exception {
        Usuario u = criarUsuarioDireto("rotacao@test.com", "Mang@Forte01", Role.BIBLIOTECARIO);
        String tokenAntigo = login(u.getEmail(), "Mang@Forte01");
        assertThat(get("/api/v1/auth/me", tokenAntigo).getStatusCode()).isEqualTo(HttpStatus.OK);

        // Espera 6s para garantir que a tolerancia de 5s da comparacao seja ultrapassada
        Thread.sleep(6_000);

        // Troca a senha
        ResponseEntity<Void> trocou = http.post().uri("/api/v1/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> h.setBearerAuth(tokenAntigo))
            .body(java.util.Map.of("senhaAtual", "Mang@Forte01", "senhaNova", "Mang@Forte99"))
            .retrieve().toEntity(Void.class);
        assertThat(trocou.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Token antigo deixa de valer (iat anterior ao passwordChangedAt)
        assertThat(get("/api/v1/auth/me", tokenAntigo).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Login com senha nova gera token valido
        String tokenNovo = login(u.getEmail(), "Mang@Forte99");
        assertThat(get("/api/v1/auth/me", tokenNovo).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuator_health_e_publico_e_responde_200() {
        ResponseEntity<String> resp = http.get().uri("/actuator/health")
            .retrieve().toEntity(String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"status\"");
    }

    @Test
    void actuator_metrics_exige_admin() {
        ResponseEntity<ProblemDetail> sem = http.get().uri("/actuator/metrics")
            .retrieve().toEntity(ProblemDetail.class);
        assertThat(sem.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Usuario admin = criarUsuarioDireto("admin-actuator@test.com", "Tr0pical#A3", Role.ADMIN);
        String token = login(admin.getEmail(), "Tr0pical#A3");
        ResponseEntity<String> com = http.get().uri("/actuator/metrics")
            .headers(h -> h.setBearerAuth(token))
            .retrieve().toEntity(String.class);
        assertThat(com.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---------------- helpers ----------------

    private Usuario criarUsuarioDireto(String email, String senha, Role role) {
        return usuarioRepository.save(Usuario.builder()
            .email(email).nome("Test User")
            .senhaHash(passwordEncoder.encode(senha))
            .role(role).ativo(true)
            .passwordChangedAt(Instant.now())
            .build());
    }

    private String login(String email, String senha) {
        ResponseEntity<LoginResponse> resp = http.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new LoginRequest(email, senha))
            .retrieve().toEntity(LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().accessToken();
    }

    private ResponseEntity<ProblemDetail> get(String uri, String token) {
        return http.get().uri(uri)
            .headers(h -> { if (token != null) h.setBearerAuth(token); })
            .retrieve().toEntity(ProblemDetail.class);
    }

    private <T> ResponseEntity<T> post(String uri, Object body, String token, Class<T> respType) {
        return http.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
            .headers(h -> { if (token != null) h.setBearerAuth(token); })
            .body(body)
            .retrieve().toEntity(respType);
    }
}
