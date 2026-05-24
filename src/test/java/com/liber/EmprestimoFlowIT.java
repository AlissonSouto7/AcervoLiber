package com.liber;

import static org.assertj.core.api.Assertions.assertThat;

import com.liber.dto.AlunoRequest;
import com.liber.dto.AlunoResponse;
import com.liber.dto.DashboardResponse;
import com.liber.dto.EmprestimoRequest;
import com.liber.dto.EmprestimoResponse;
import com.liber.dto.LivroRequest;
import com.liber.dto.LivroResponse;
import com.liber.dto.auth.LoginRequest;
import com.liber.dto.auth.LoginResponse;
import com.liber.dto.auth.RegisterRequest;
import com.liber.dto.auth.UsuarioResponse;
import com.liber.entity.SituacaoEmprestimo;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Teste de integracao end-to-end do fluxo principal:
 * registro → login → cadastro de livro/aluno → emprestimo → estoque → devolucao.
 */
class EmprestimoFlowIT extends AbstractIntegrationTest {

    @LocalServerPort int port;

    private RestClient http;

    @BeforeEach
    void initClient() {
        http = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            // Nao lanca em 4xx/5xx — queremos asseverar status manualmente
            .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> { })
            .build();
    }

    @Test
    void fluxo_completo_emprestimo_e_devolucao() {
        // 1) Registra um bibliotecario
        ResponseEntity<UsuarioResponse> reg = post("/api/v1/auth/register",
            new RegisterRequest("biblio1@test.com", "Maria", "senha-segura-1"),
            null, UsuarioResponse.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2) Login obtem token
        String token = login("biblio1@test.com", "senha-segura-1");

        // 3) Endpoint protegido sem token retorna 401
        ResponseEntity<ProblemDetail> semToken = post("/api/v1/livros",
            new LivroRequest("X", "Y", null, 2020, 1, null), null, ProblemDetail.class);
        assertThat(semToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 4) Cria livro com 2 exemplares
        LivroResponse livro = expectCreated(post("/api/v1/livros",
            new LivroRequest("Dom Casmurro", "Machado de Assis", null, 1899, 2, null),
            token, LivroResponse.class));
        assertThat(livro.quantidadeExemplares()).isEqualTo(2);
        assertThat(livro.quantidadeDisponivel()).isEqualTo(2);

        // 5) Cria aluno
        AlunoResponse aluno = expectCreated(post("/api/v1/alunos",
            new AlunoRequest("2026001", "Pedro Silva", "9A"),
            token, AlunoResponse.class));

        // 6) Primeiro emprestimo — estoque cai para 1
        EmprestimoResponse emp1 = expectCreated(post("/api/v1/emprestimos",
            new EmprestimoRequest(livro.id(), aluno.id(), 7),
            token, EmprestimoResponse.class));
        assertThat(emp1.situacao()).isEqualTo(SituacaoEmprestimo.ATIVO);
        assertThat(get("/api/v1/livros/" + livro.id(), token, LivroResponse.class).quantidadeDisponivel()).isEqualTo(1);

        // 7) Segundo emprestimo — estoque cai para 0
        EmprestimoResponse emp2 = expectCreated(post("/api/v1/emprestimos",
            new EmprestimoRequest(livro.id(), aluno.id(), 7),
            token, EmprestimoResponse.class));
        assertThat(emp2.situacao()).isEqualTo(SituacaoEmprestimo.ATIVO);
        assertThat(get("/api/v1/livros/" + livro.id(), token, LivroResponse.class).quantidadeDisponivel()).isZero();

        // 8) Terceiro emprestimo falha (estoque indisponivel — 409)
        ResponseEntity<ProblemDetail> falha = post("/api/v1/emprestimos",
            new EmprestimoRequest(livro.id(), aluno.id(), 7),
            token, ProblemDetail.class);
        assertThat(falha.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 9) Devolucao do primeiro — estoque volta para 1
        ResponseEntity<EmprestimoResponse> devol = postEmpty("/api/v1/emprestimos/" + emp1.id() + "/devolucao",
            token, EmprestimoResponse.class);
        assertThat(devol.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(devol.getBody().situacao()).isEqualTo(SituacaoEmprestimo.DEVOLVIDO);
        assertThat(get("/api/v1/livros/" + livro.id(), token, LivroResponse.class).quantidadeDisponivel()).isEqualTo(1);

        // 10) Devolver de novo o mesmo emprestimo falha (422)
        ResponseEntity<ProblemDetail> dobro = postEmpty("/api/v1/emprestimos/" + emp1.id() + "/devolucao",
            token, ProblemDetail.class);
        assertThat(dobro.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);

        // 11) Dashboard reflete o estado atual
        DashboardResponse dash = get("/api/v1/dashboard", token, DashboardResponse.class);
        assertThat(dash.totais().totalLivros()).isEqualTo(1);
        assertThat(dash.totais().totalAlunos()).isEqualTo(1);
        assertThat(dash.totais().emprestimosAtivos()).isEqualTo(1);
    }

    @Test
    void prazo_acima_do_maximo_e_rejeitado_com_422() {
        post("/api/v1/auth/register",
            new RegisterRequest("biblio2@test.com", "Ana", "senha-segura-2"),
            null, UsuarioResponse.class);
        String token = login("biblio2@test.com", "senha-segura-2");

        LivroResponse livro = expectCreated(post("/api/v1/livros",
            new LivroRequest("X", "Y", null, 2020, 5, null),
            token, LivroResponse.class));
        AlunoResponse aluno = expectCreated(post("/api/v1/alunos",
            new AlunoRequest("2026002", "Joao", "9B"),
            token, AlunoResponse.class));

        ResponseEntity<ProblemDetail> resp = post("/api/v1/emprestimos",
            new EmprestimoRequest(livro.id(), aluno.id(), 999), token, ProblemDetail.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void change_password_funciona_e_senha_antiga_deixa_de_valer() {
        post("/api/v1/auth/register",
            new RegisterRequest("biblio4@test.com", "Lucia", "senha-original"),
            null, UsuarioResponse.class);
        String token = login("biblio4@test.com", "senha-original");

        ResponseEntity<Void> trocou = post("/api/v1/auth/change-password",
            Map.of("senhaAtual", "senha-original", "senhaNova", "senha-nova-1"),
            token, Void.class);
        assertThat(trocou.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ProblemDetail> falhaLogin = post("/api/v1/auth/login",
            new LoginRequest("biblio4@test.com", "senha-original"), null, ProblemDetail.class);
        assertThat(falhaLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<LoginResponse> sucesso = post("/api/v1/auth/login",
            new LoginRequest("biblio4@test.com", "senha-nova-1"), null, LoginResponse.class);
        assertThat(sucesso.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---------------- helpers ----------------

    private String login(String email, String senha) {
        ResponseEntity<LoginResponse> resp = post("/api/v1/auth/login",
            new LoginRequest(email, senha), null, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().accessToken();
    }

    private <T> ResponseEntity<T> post(String uri, Object body, String token, Class<T> respType) {
        return http.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> { if (token != null) h.setBearerAuth(token); })
            .body(body)
            .retrieve()
            .toEntity(respType);
    }

    private <T> ResponseEntity<T> postEmpty(String uri, String token, Class<T> respType) {
        return http.post()
            .uri(uri)
            .headers(h -> { if (token != null) h.setBearerAuth(token); })
            .retrieve()
            .toEntity(respType);
    }

    private <T> T get(String uri, String token, Class<T> respType) {
        ResponseEntity<T> resp = http.get()
            .uri(uri)
            .headers(h -> { if (token != null) h.setBearerAuth(token); })
            .retrieve()
            .toEntity(respType);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    private static <T> T expectCreated(ResponseEntity<T> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }
}
