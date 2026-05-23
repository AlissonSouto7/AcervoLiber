package com.liber.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liber.config.RateLimitProperties;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitingFilterTest {

    private static final String LOGIN_URI = "/api/v1/auth/login";

    private RateLimitProperties props;
    private RateLimitingFilter filter;
    private FilterChain chain;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        props = new RateLimitProperties(true, 3, 3, 60); // 3 attempts per IP
        filter = new RateLimitingFilter(props, new ObjectMapper());

        chain = org.mockito.Mockito.mock(FilterChain.class);
        request = org.mockito.Mockito.mock(HttpServletRequest.class);
        response = org.mockito.Mockito.mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn(LOGIN_URI);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    void permite_ate_capacity_e_bloqueia_o_proximo() throws Exception {
        // 3 tentativas — todas permitidas
        filter.doFilter(request, response, chain);
        filter.doFilter(request, response, chain);
        filter.doFilter(request, response, chain);
        verify(chain, times(3)).doFilter(any(), any());

        // 4a tentativa do mesmo IP — 429
        filter.doFilter(request, response, chain);
        verify(chain, times(3)).doFilter(any(), any()); // nao chamou de novo
        verify(response).setStatus(429);
        verify(response, atLeastOnce()).setHeader(org.mockito.ArgumentMatchers.eq("Retry-After"), any());
        assertThat(responseBody.toString()).contains("Rate limit excedido");
    }

    @Test
    void ips_diferentes_tem_buckets_independentes() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        for (int i = 0; i < 3; i++) filter.doFilter(request, response, chain);

        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        filter.doFilter(request, response, chain); // outro IP — permitido

        verify(chain, times(4)).doFilter(any(), any());
    }

    @Test
    void path_fora_de_login_nunca_e_limitado() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/livros");
        for (int i = 0; i < 50; i++) filter.doFilter(request, response, chain);
        verify(chain, times(50)).doFilter(any(), any());
    }

    @Test
    void quando_desabilitado_nada_e_limitado() throws Exception {
        props = new RateLimitProperties(false, 1, 1, 60);
        filter = new RateLimitingFilter(props, new ObjectMapper());

        for (int i = 0; i < 10; i++) filter.doFilter(request, response, chain);
        verify(chain, times(10)).doFilter(any(), any());
        verify(response, never()).setStatus(429);
    }
}
