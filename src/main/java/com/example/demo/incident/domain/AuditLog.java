package com.example.demo.incident.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "AuditLogs", indexes = {
        @Index(name = "ix_audit_alert", columnList = "alert_id"),
        @Index(name = "ix_audit_service_created", columnList = "service_name, created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false, length = 50)
    private String node;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(Long alertId, String serviceName, String node, String action, String detail) {
        this.alertId = alertId;
        this.serviceName = serviceName;
        this.node = node;
        this.action = action;
        this.detail = detail != null && detail.length() > 2000 ? detail.substring(0, 2000) : detail;
    }

    public Long getId() {
        return id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getNode() {
        return node;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
