package com.liber.repository;

import com.liber.entity.AuditLog;
import com.liber.entity.EventoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByOcorridoEmDesc(Pageable pageable);

    Page<AuditLog> findByEventoOrderByOcorridoEmDesc(EventoAuditoria evento, Pageable pageable);
}
