package com.example.demo.incident;

import com.example.demo.incident.domain.Alert;
import com.example.demo.incident.domain.AlertRepository;
import com.example.demo.incident.domain.AuditLog;
import com.example.demo.incident.domain.AuditLogRepository;
import com.example.demo.incident.graph.IncidentGraphConfig;
import com.example.demo.incident.graph.IncidentState;
import com.example.demo.incident.graph.RemediationStep;
import com.example.demo.incident.graph.SimulatedEnvironment;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    public record IncidentView(String incidentId, String service, String status, boolean awaitingApproval,
                               List<String> findings, List<String> history, List<RemediationStep> plan,
                               List<String> timeline) {
    }

    private final CompiledGraph<IncidentState> graph;
    private final AlertRepository alerts;
    private final AuditLogRepository audit;
    private final SimulatedEnvironment env;

    public IncidentService(CompiledGraph<IncidentState> graph, AlertRepository alerts,
                           AuditLogRepository audit, SimulatedEnvironment env) {
        this.graph = graph;
        this.alerts = alerts;
        this.audit = audit;
        this.env = env;
    }

    public IncidentView open(String serviceName, String message) {
        env.seedIfAbsent(serviceName);
        Alert alert = alerts.save(new Alert(serviceName, message));
        String id = alert.getId().toString();
        run(GraphInput.args(Map.of(
                IncidentState.INCIDENT_ID, id,
                IncidentState.SERVICE, serviceName,
                IncidentState.ALERT, message)), config(id));
        return view(id);
    }

    public IncidentView decide(String id, boolean approved) throws Exception {
        if (!isAwaitingApproval(id)) {
            throw new IllegalStateException("Incident " + id + " is not waiting for approval");
        }
        RunnableConfig updated = graph.updateState(config(id),
                Map.of(IncidentState.APPROVAL, approved ? IncidentState.APPROVED : IncidentState.REJECTED));
        run(GraphInput.resume(), updated);
        return view(id);
    }

    public IncidentView view(String id) {
        var snapshot = graph.getState(config(id));
        IncidentState s = snapshot.state();
        return new IncidentView(id, s.service(), s.status(), IncidentGraphConfig.APPROVAL_GATE.equals(snapshot.next()),
                s.findings(), s.history(), s.plan(), s.timeline());
    }

    public List<AuditLog> auditTrail(String id) {
        return audit.findByAlertIdOrderByIdAsc(Long.valueOf(id));
    }

    private boolean isAwaitingApproval(String id) {
        return IncidentGraphConfig.APPROVAL_GATE.equals(graph.getState(config(id)).next());
    }

    private void run(GraphInput input, RunnableConfig config) {
        // Drain the stream: it ends either at END or at the approval interrupt.
        for (var ignored : graph.stream(input, config)) {
        }
        syncAlertStatus(config.threadId().orElseThrow());
    }

    private void syncAlertStatus(String id) {
        String status = graph.getState(config(id)).state().status();
        String alertStatus = switch (status) {
            case "RESOLVED", "REJECTED", "ESCALATED" -> status;
            default -> Alert.OPEN;
        };
        alerts.findById(Long.valueOf(id)).ifPresent(a -> {
            a.setStatus(alertStatus);
            alerts.save(a);
        });
    }

    private static RunnableConfig config(String id) {
        return RunnableConfig.builder().threadId(id).build();
    }
}
