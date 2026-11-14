package com.example.demo.incident.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** One sample of a service's health; also carries the simulated infrastructure state (replicas, leak). */
@Entity
@Table(name = "ServiceMetrics", indexes = {
        @Index(name = "ix_metrics_service_created", columnList = "service_name, created_at")
})
public class ServiceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "cpu_percent", nullable = false)
    private double cpuPercent;

    @Column(name = "pool_active", nullable = false)
    private int poolActive;

    @Column(name = "pool_max", nullable = false)
    private int poolMax;

    @Column(nullable = false)
    private int replicas;

    @Column(name = "connection_leak", nullable = false)
    private boolean connectionLeak;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ServiceMetric() {
    }

    public ServiceMetric(String serviceName, double cpuPercent, int poolActive, int poolMax,
                         int replicas, boolean connectionLeak) {
        this.serviceName = serviceName;
        this.cpuPercent = cpuPercent;
        this.poolActive = poolActive;
        this.poolMax = poolMax;
        this.replicas = replicas;
        this.connectionLeak = connectionLeak;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getCpuPercent() {
        return cpuPercent;
    }

    public int getPoolActive() {
        return poolActive;
    }

    public int getPoolMax() {
        return poolMax;
    }

    public int getReplicas() {
        return replicas;
    }

    public boolean isConnectionLeak() {
        return connectionLeak;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public double poolUsage() {
        return (double) poolActive / poolMax;
    }
}
