package com.liber.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(EstoqueIndisponivelException.class)
    public ProblemDetail handleEstoque(EstoqueIndisponivelException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Estoque indisponivel", ex.getMessage(), req);
    }

    @ExceptionHandler(RegraEmprestimoException.class)
    public ProblemDetail handleRegraEmprestimo(RegraEmprestimoException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de emprestimo violada", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negocio violada", ex.getMessage(), req);
    }

    @ExceptionHandler(RegistroPublicoDesabilitadoException.class)
    public ProblemDetail handleRegistroDesabilitado(RegistroPublicoDesabilitadoException ex,
                                                     HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Registro desabilitado", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of(
                "campo", fe.getField(),
                "mensagem", fe.getDefaultMessage() == null ? "invalido" : fe.getDefaultMessage()))
            .collect(Collectors.toList());

        // Detail agora carrega a primeira mensagem especifica do validator (ex.:
        // "Senha deve conter ao menos uma letra MAIUSCULA.") em vez do generico
        // "Um ou mais campos...". O frontend mostra esse detail direto no toast.
        String detail = erros.isEmpty()
            ? "Um ou mais campos da requisicao sao invalidos."
            : erros.get(0).get("mensagem");

        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Dados invalidos", detail, req);
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Parametro '%s' com valor invalido: '%s'".formatted(ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, "Parametro invalido", msg, req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais invalidas",
            "Email ou senha incorretos.", req);
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ProblemDetail handleRefreshTokenInvalido(RefreshTokenInvalidoException ex,
                                                     HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Refresh token invalido", ex.getMessage(), req);
    }

    @ExceptionHandler(ContaBloqueadaException.class)
    public ProblemDetail handleContaBloqueada(ContaBloqueadaException ex, HttpServletRequest req) {
        return build(HttpStatus.LOCKED, "Conta bloqueada", ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Falha de autenticacao",
            "Autenticacao necessaria ou invalida.", req);
    }

    /**
     * Acesso negado pela autorizacao em nivel de metodo ({@code @PreAuthorize}).
     * Sem este handler a excecao escaparia para o handler generico e viraria 500 —
     * o cliente deve receber 403 (proibido), nao 500 (erro interno).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Acesso negado: {} {}", req.getMethod(), req.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "Acesso negado",
            "Voce nao tem permissao para acessar este recurso.", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String msg = ex.getMostSpecificCause().getMessage();
        log.warn("Violacao de integridade: {}", msg);
        // UNIQUE parcial da reserva pendente — caso ja coberto pelo lock pessimista
        // do ReservaService, mas o DB e a defesa final. Devolvemos 422 com mensagem
        // amigavel ao inves do 409 generico.
        if (msg != null && msg.contains("uq_reservas_aluno_livro_pendente")) {
            return build(HttpStatus.UNPROCESSABLE_CONTENT, "Reserva duplicada",
                "Voce ja tem uma reserva pendente para este livro.", req);
        }
        return build(HttpStatus.CONFLICT, "Conflito de dados",
            "A operacao viola uma restricao do banco (chave duplicada, FK, etc.).", req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflito de concorrencia",
            "O recurso foi alterado por outra requisicao. Tente novamente.", req);
    }

    /**
     * Path que nao corresponde a nenhum controller — Spring MVC lanca
     * NoResourceFoundException. Sem este handler, escala pro handleGeneric e
     * vira HTTP 500 "Erro interno", confundindo bug real com 404. O pentest da
     * Fase 7 explorou isso: GET /api/v1/usuarios/{id} (endpoint inexistente)
     * retornava 500, mascarando que o path estava errado.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso nao encontrado",
            "O caminho solicitado nao existe.", req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro nao tratado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
            "Ocorreu um erro inesperado. Tente novamente em instantes.", req);
    }

    private ProblemDetail build(HttpStatus status, String title, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }
}
