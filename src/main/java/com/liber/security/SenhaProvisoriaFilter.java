package com.liber.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Bloqueia, no servidor, qualquer usuario que ainda esteja com senha provisoria
 * ({@code deveTrocarSenha=true}). Sem isto, o gate de troca de senha existiria
 * apenas no frontend e poderia ser contornado chamando a API diretamente.
 *
 * <p>Enquanto a senha provisoria nao for trocada, somente os endpoints de
 * autenticacao ({@code /api/v1/auth/**} — onde mora o {@code change-password})
 * permanecem acessiveis. Roda depois do {@link JwtAuthenticationFilter}, pois
 * depende do principal ja autenticado no {@link SecurityContextHolder}.
 */
@Component
@RequiredArgsConstructor
public class SenhaProvisoriaFilter extends OncePerRequestFilter {

    /** Prefixo liberado: endpoints de auth (login, me, refresh, logout, change-password, perfil). */
    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.getPrincipal() instanceof AppUserDetails user
                && user.isDeveTrocarSenha()
                && !request.getRequestURI().startsWith(AUTH_PREFIX)) {
            escreverBloqueio(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void escreverBloqueio(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Troque a senha provisoria antes de continuar usando o sistema.");
        pd.setTitle("Troca de senha obrigatoria");
        pd.setType(URI.create("about:blank"));
        pd.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
