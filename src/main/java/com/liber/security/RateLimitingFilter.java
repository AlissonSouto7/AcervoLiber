package com.liber.security;

import com.liber.config.RateLimitProperties;
import tools.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit por IP no endpoint de login — token bucket via bucket4j.
 * In-memory (single instance). Para multi-instancia, trocar por bucket4j-redis.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    // Cobre /api/v1/auth/login e /api/v1/auth/login-aluno
    private static final String LOGIN_PATH_PREFIX = "/api/v1/auth/login";

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!props.enabled() || !request.getRequestURI().startsWith(LOGIN_PATH_PREFIX)
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Usa o IP que o Tomcat resolveu — com server.forward-headers-strategy=native +
        // internal-proxies configurados, o RemoteIpValve substitui getRemoteAddr() pelo
        // IP real do cliente apenas quando o request vem de um proxy confiavel. Assim
        // um atacante na internet NAO consegue spoofar via X-Forwarded-For.
        String clientId = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit excedido no login para IP={}", clientId);
        writeTooManyRequests(request, response);
    }

    private Bucket newBucket() {
        Bandwidth bw = Bandwidth.builder()
            .capacity(props.capacity())
            .refillGreedy(props.refillTokens(), Duration.ofSeconds(props.refillPeriodSeconds()))
            .build();
        return Bucket.builder().addLimit(bw).build();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
            "Muitas tentativas de login. Tente novamente em alguns instantes.");
        pd.setTitle("Rate limit excedido");
        pd.setType(URI.create("about:blank"));
        pd.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(props.refillPeriodSeconds()));
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
