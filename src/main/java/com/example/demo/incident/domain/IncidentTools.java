package com.example.demo.incident.domain;

import org.springframework.stereotype.Component;

import java.util.List;

/** Tools exposed to the agent/LLM; they read real rows from SQL Server. */
@Component
public class IncidentTools {

    private final AlertRepository alerts;
    private final ServiceMetricRepository metrics;

    public IncidentTools(AlertRepository alerts, ServiceMetricRepository metrics) {
        this.alerts = alerts;
        this.metrics = metrics;
    }

    /** query_active_incidents() */
    public List<String> queryActiveIncidents() {
        return alerts.findTop20ByStatusOrderByIdDesc(Alert.OPEN).stream()
                .map(a -> "#%d %s: %s".formatted(a.getId(), a.getServiceName(), a.getMessage()))
                .toList();
    }

    /** fetch_service_metrics(service_name): most recent samples, newest first. */
    public List<ServiceMetric> fetchServiceMetrics(String serviceName) {
        return metrics.findTop10ByServiceNameOrderByIdDesc(serviceName);
    }

    public static String describe(ServiceMetric m) {
        return "cpu=%.0f%% pool=%d/%d replicas=%d".formatted(
                m.getCpuPercent(), m.getPoolActive(), m.getPoolMax(), m.getReplicas());
    }
}
