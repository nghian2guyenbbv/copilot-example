package com.example.demo.incident.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "Alerts", indexes = {
        @Index(name = "ix_alerts_service_created", columnList = "service_name, created_at"),
        @Index(name = "ix_alerts_status", columnList = "status")
})
public class Alert {

    public static final String OPEN = "OPEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 30)
    private String status = OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Alert() {
    }

    public Alert(String serviceName, String message) {
        this.serviceName = serviceName;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
