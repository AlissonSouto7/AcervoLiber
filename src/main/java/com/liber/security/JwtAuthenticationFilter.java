package com.liber.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            String email = jwtService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)
                        && userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()
                        && userDetails.isAccountNonExpired()
                        && userDetails.isCredentialsNonExpired()
                        && tokenEmitidoAposUltimaTrocaDeSenha(token, userDetails)) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (JwtException | UsernameNotFoundException e) {
            log.debug("Falha ao autenticar via JWT: {}", e.getMessage());
            // Deixa a request seguir sem autenticacao — entry point retornara 401
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Compara o {@code iat} do token com {@code passwordChangedAt} do usuario.
     * Tokens emitidos antes da ultima troca de senha sao considerados invalidos.
     * Tolerancia de 5s para acomodar arredondamento entre Instant e segundos do JWT.
     */
    private boolean tokenEmitidoAposUltimaTrocaDeSenha(String token, UserDetails userDetails) {
        if (!(userDetails instanceof AppUserDetails app)) {
            return true; // se nao for nosso UserDetails, nao temos como comparar
        }
        try {
            java.time.Instant iat = jwtService.extractIssuedAt(token);
            return !iat.plusSeconds(5).isBefore(app.getPasswordChangedAt());
        } catch (Exception e) {
            log.debug("Falha ao extrair iat do token: {}", e.getMessage());
            return false;
        }
    }
}
