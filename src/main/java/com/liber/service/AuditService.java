package com.liber.service;

import com.liber.entity.AuditLog;
import com.liber.entity.EventoAuditoria;
import com.liber.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registro e consulta da trilha de auditoria de seguranca.
 *
 * <p>{@link #registrar} roda em transacao propria ({@code REQUIRES_NEW}): o evento
 * e gravado mesmo que a operacao principal falhe e seja revertida — essencial para
 * registrar tentativas de login malsucedidas.
 *
 * <p>O <b>ator</b> (quem fez a acao) e capturado automaticamente do
 * {@link SecurityContextHolder} — eventos administrativos passam a identificar
 * autor + alvo, nao so o alvo. Eventos sem auth (LOGIN_FALHA, REFRESH_REUSO)
 * tem ator NULL, comportamento esperado.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EventoAuditoria evento, String usuarioEmail, String detalhe) {
        HttpServletRequest req = requestAtual();
        repository.save(AuditLog.builder()
            .evento(evento)
            .usuarioEmail(usuarioEmail)
            .atorEmail(atorAtual())
            .ip(req != null ? req.getRemoteAddr() : null)
            .userAgent(req != null ? truncar(req.getHeader("User-Agent"), 255) : null)
            .detalhe(truncar(detalhe, 500))
            .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> consultar(EventoAuditoria evento, Pageable pageable) {
        return evento != null
            ? repository.findByEventoOrderByOcorridoEmDesc(evento, pageable)
            : repository.findAllByOrderByOcorridoEmDesc(pageable);
    }

    private static HttpServletRequest requestAtual() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    /**
     * Email do sujeito autenticado no momento — capturado do SecurityContextHolder.
     * Retorna null se nao houver auth (endpoints publicos, jobs, startup).
     * O `clientIp()` separado de `AuthService` ja faz isto pelo IP; aqui replicamos
     * para o ator humano.
     */
    private static String atorAtual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                return auth.getName();
            }
        } catch (RuntimeException ignored) {
            // Sem contexto disponivel — sem ator
        }
        return null;
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
