package com.liber.config;

import com.liber.security.JwtAuthenticationFilter;
import com.liber.security.RateLimitingFilter;
import com.liber.security.RestAccessDeniedHandler;
import com.liber.security.RestAuthenticationEntryPoint;
import com.liber.security.SenhaProvisoriaFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/login-aluno",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        // /api/v1/auth/logout NAO e mais publico — exige autenticacao + validacao
        // de ownership do refresh token (senao qualquer um revoga sessao alheia).
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info"
    };

    /** Imagem de capa — publica (carregada por <img>, sem cabecalho de auth). */
    private static final String CAPA_IMAGEM_ENDPOINT = "/api/v1/livros/*/capa-imagem";

    /** Custo do BCrypt (2^strength rounds). 12 e o recomendado atual; 10 e o default da lib. */
    private static final int BCRYPT_STRENGTH = 12;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SenhaProvisoriaFilter senhaProvisoriaFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final UserDetailsService userDetailsService;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                // HSTS — so e enviado sobre HTTPS; forca o browser a usar TLS por 1 ano
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000))
                // API nao deve ser renderizada em frames
                .frameOptions(frame -> frame.deny())
                // Impede o browser de "adivinhar" content-type (anti MIME-sniffing)
                .contentTypeOptions(contentType -> { })
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, CAPA_IMAGEM_ENDPOINT).permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Apos o JWT: barra usuarios com senha provisoria (deveTrocarSenha=true)
            .addFilterAfter(senhaProvisoriaFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // setAllowedOriginPatterns (em vez de setAllowedOrigins) aceita curingas
        // como "http://localhost:*" — cobre o Vite em qualquer porta sem precisar
        // reconfigurar. Origens exatas tambem funcionam aqui (viram match exato).
        cfg.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        cfg.setAllowedMethods(corsProperties.allowedMethods());
        cfg.setAllowedHeaders(corsProperties.allowedHeaders());
        cfg.setAllowCredentials(corsProperties.allowCredentials());
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
