package com.liber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liber.entity.AuditLog;
import com.liber.entity.EventoAuditoria;
import com.liber.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository repository;
    @InjectMocks AuditService service;

    @Test
    void registrar_persiste_evento_com_email_e_detalhe() {
        service.registrar(EventoAuditoria.LOGIN_FALHA, "x@y.com", "Credenciais invalidas");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog salvo = captor.getValue();

        assertThat(salvo.getEvento()).isEqualTo(EventoAuditoria.LOGIN_FALHA);
        assertThat(salvo.getUsuarioEmail()).isEqualTo("x@y.com");
        assertThat(salvo.getDetalhe()).isEqualTo("Credenciais invalidas");
        // Sem contexto de request num teste unitario — ip/userAgent ficam nulos
        assertThat(salvo.getIp()).isNull();
        assertThat(salvo.getUserAgent()).isNull();
    }

    @Test
    void registrar_trunca_detalhe_longo_em_500_caracteres() {
        service.registrar(EventoAuditoria.LOGIN_SUCESSO, "a@b.com", "x".repeat(600));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDetalhe()).hasSize(500);
    }

    @Test
    void consultar_sem_filtro_usa_listagem_geral() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAllByOrderByOcorridoEmDesc(pageable)).thenReturn(Page.empty());

        service.consultar(null, pageable);

        verify(repository).findAllByOrderByOcorridoEmDesc(pageable);
    }

    @Test
    void consultar_com_filtro_usa_busca_por_evento() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByEventoOrderByOcorridoEmDesc(EventoAuditoria.LOGOUT, pageable))
            .thenReturn(Page.empty());

        service.consultar(EventoAuditoria.LOGOUT, pageable);

        verify(repository).findByEventoOrderByOcorridoEmDesc(EventoAuditoria.LOGOUT, pageable);
    }
}
