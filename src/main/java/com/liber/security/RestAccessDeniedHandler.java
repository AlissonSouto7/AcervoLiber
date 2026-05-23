package com.liber.security;

import com.liber.entity.EventoAuditoria;
import com.liber.service.AuditService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // Registra na trilha: usuario autenticado tentou acessar algo sem permissao.
        // E sinal forte de probing/insider — sem isto, ALUNO testando endpoints
        // de gestao passava em silencio. O AuditService captura o ator do
        // SecurityContext; alvo = path tentado.
        auditService.registrar(EventoAuditoria.ACESSO_NEGADO, null,
            request.getMethod() + " " + request.getRequestURI());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Voce nao tem permissao para acessar este recurso.");
        pd.setTitle("Acesso negado");
        pd.setType(URI.create("about:blank"));
        pd.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
