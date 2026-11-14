package com.example.demo.incident.graph;

import com.example.demo.incident.domain.Runbook;
import com.example.demo.incident.domain.RunbookRepository;
import com.example.demo.incident.domain.ServiceMetric;
import com.example.demo.incident.domain.ServiceMetricRepository;
import com.example.demo.incident.llm.ActionType;
import org.springframework.stereotype.Component;

/**
 * Simulated infrastructure for a service. Its state lives in SQL Server: every action appends a new
 * ServiceMetrics row derived from the latest one. Replace with real kubectl/psql executors in production.
 */
@Component
public class SimulatedEnvironment {

    private static final double TOTAL_LOAD = 184.0; // cpu% = load / replicas

    private final ServiceMetricRepository metrics;
    private final RunbookRepository runbooks;

    public SimulatedEnvironment(ServiceMetricRepository metrics, RunbookRepository runbooks) {
        this.metrics = metrics;
        this.runbooks = runbooks;
    }

    /** First alert for a service: start it unhealthy (high CPU, exhausted pool, leaking connections). */
    public void seedIfAbsent(String service) {
        if (metrics.findFirstByServiceNameOrderByIdDesc(service).isEmpty()) {
            metrics.save(new ServiceMetric(service, TOTAL_LOAD / 2, 20, 20, 2, true));
        }
        if (runbooks.findByServiceName(service).isEmpty()) {
            runbooks.save(new Runbook(service, ActionType.SCALE_OUT.name(), "Scale out when CPU > 70%."));
            runbooks.save(new Runbook(service, ActionType.KILL_IDLE_SESSIONS.name(),
                    "Terminate idle-in-transaction DB sessions; needs approval."));
            runbooks.save(new Runbook(service, ActionType.RESTART_SERVICE.name(),
                    "Rolling restart clears leaked connections; needs approval."));
        }
    }

    public String execute(String service, ActionType action) {
        ServiceMetric m = metrics.findFirstByServiceNameOrderByIdDesc(service).orElseThrow();
        ServiceMetric next = switch (action) {
            case SCALE_OUT -> new ServiceMetric(service, TOTAL_LOAD / 4, m.getPoolActive(), m.getPoolMax(), 4,
                    m.isConnectionLeak());
            // The leak refills the pool right after the sessions are killed.
            case KILL_IDLE_SESSIONS -> new ServiceMetric(service, m.getCpuPercent(), 18, m.getPoolMax(),
                    m.getReplicas(), m.isConnectionLeak());
            case RESTART_SERVICE -> new ServiceMetric(service, m.getCpuPercent(), 3, m.getPoolMax(),
                    m.getReplicas(), false);
            case NONE -> m;
        };
        if (next != m) {
            metrics.save(next);
        }
        return "OK: " + action.command(service);
    }
}
