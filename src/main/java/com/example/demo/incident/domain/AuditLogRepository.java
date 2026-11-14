package com.example.demo.incident.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAlertIdOrderByIdAsc(Long alertId);

    List<AuditLog> findTop20ByServiceNameAndAlertIdNotAndActionStartingWithOrderByIdDesc(
            String serviceName, Long alertId, String actionPrefix);
}
