package com.example.demo.incident.graph;

import com.example.demo.incident.domain.*;
import com.example.demo.incident.llm.ActionType;
import com.example.demo.incident.llm.Decision;
import com.example.demo.incident.llm.DecisionContext;
import com.example.demo.incident.llm.DecisionService;
import com.example.demo.incident.llm.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.demo.incident.graph.IncidentState.*;

@Component
public class IncidentNodes {

    static final String EXECUTED_PREFIX = "EXECUTED_";
    static final double HEALTHY_CPU = 70;
    static final double HEALTHY_POOL = 0.7;

    private final IncidentTools tools;
    private final AuditLogRepository audit;
    private final RunbookRepository runbooks;
    private final ServiceMetricRepository metrics;
    private final DecisionService decisions;
    private final SimulatedEnvironment env;

    public IncidentNodes(IncidentTools tools, AuditLogRepository audit, RunbookRepository runbooks,
                         ServiceMetricRepository metrics, DecisionService decisions, SimulatedEnvironment env) {
        this.tools = tools;
        this.audit = audit;
        this.runbooks = runbooks;
        this.metrics = metrics;
        this.decisions = decisions;
        this.env = env;
    }

    private void log(IncidentState s, String node, String action, String detail) {
        audit.save(new AuditLog(Long.valueOf(s.incidentId()), s.service(), node, action, detail));
    }

    public Map<String, Object> dispatch(IncidentState s) {
        return Map.of(STATUS, "INVESTIGATING",
                TIMELINE, List.of("dispatch: parallel investigation (attempt " + (s.attempt() + 1) + ")"));
    }

    // The three lookups below run in parallel in one superstep; each writes a different state key.

    public Map<String, Object> lookupHistory(IncidentState s) {
        List<String> past = audit.findTop20ByServiceNameAndAlertIdNotAndActionStartingWithOrderByIdDesc(
                        s.service(), Long.valueOf(s.incidentId()), EXECUTED_PREFIX).stream()
                .map(a -> "alert #%d: %s".formatted(a.getAlertId(), a.getAction().substring(EXECUTED_PREFIX.length())))
                .toList();
        log(s, "lookup_history", "LOOKUP", past.size() + " past action(s)");
        return Map.of(HISTORY, past);
    }

    public Map<String, Object> lookupRunbooks(IncidentState s) {
        List<String> rb = runbooks.findByServiceName(s.service()).stream()
                .map(r -> r.getActionType() + ": " + r.getInstructions())
                .toList();
        log(s, "lookup_runbooks", "LOOKUP", rb.size() + " runbook(s)");
        return Map.of(RUNBOOKS, rb);
    }

    public Map<String, Object> scanMetrics(IncidentState s) {
        List<ServiceMetric> samples = tools.fetchServiceMetrics(s.service());
        List<String> findings = new ArrayList<>();
        if (!samples.isEmpty()) {
            ServiceMetric m = samples.get(0);
            findings.add(IncidentTools.describe(m));
            if (m.getCpuPercent() >= HEALTHY_CPU) {
                findings.add("WARN cpu high");
            }
            if (m.poolUsage() >= HEALTHY_POOL) {
                findings.add("ERROR HikariPool-1 - Connection is not available, request timed out");
            }
            if (m.isConnectionLeak()) {
                findings.add("WARN connections held 'idle in transaction' (leak suspected)");
            }
        }
        log(s, "scan_metrics", "LOOKUP", String.join(" | ", findings));
        return Map.of(FINDINGS, findings, TIMELINE, List.of("investigate: " + String.join(" | ", findings)));
    }

    public Map<String, Object> decide(IncidentState s) {
        List<ServiceMetric> samples = tools.fetchServiceMetrics(s.service());
        ServiceMetric latest = samples.get(0);
        List<String> done = audit.findByAlertIdOrderByIdAsc(Long.valueOf(s.incidentId())).stream()
                .filter(a -> a.getAction().startsWith(EXECUTED_PREFIX))
                .map(a -> a.getAction().substring(EXECUTED_PREFIX.length()))
                .toList();
        Decision d = decisions.decide(new DecisionContext(
                s.service(), s.alert(), tools.queryActiveIncidents(),
                samples.stream().map(IncidentTools::describe).toList(), done, s.runbooks(),
                latest.getCpuPercent() >= HEALTHY_CPU, latest.poolUsage() >= HEALTHY_POOL));

        boolean dangerous = d.riskLevel() == RiskLevel.HIGH;
        List<RemediationStep> plan = d.actionType() == ActionType.NONE ? List.of()
                : List.of(new RemediationStep(d.actionType().command(s.service()), dangerous, d.actionType().name()));
        log(s, "decide", "DECIDED", d.actionType() + "/" + d.riskLevel());
        return Map.of(
                ACTION_TYPE, d.actionType().name(),
                RISK_LEVEL, d.riskLevel().name(),
                PLAN, plan,
                APPROVAL, PENDING,
                ATTEMPT, s.attempt() + 1,
                STATUS, "PLANNED",
                TIMELINE, List.of("plan (attempt %d): %s risk=%s".formatted(s.attempt() + 1, d.actionType(), d.riskLevel())));
    }

    public boolean needsApproval(IncidentState s) {
        return RiskLevel.HIGH.name().equals(s.riskLevel()) && !s.plan().isEmpty();
    }

    public Map<String, Object> gate(IncidentState s) {
        boolean ok = APPROVED.equals(s.approval());
        log(s, "approval_gate", s.approval(), s.actionType());
        return Map.of(STATUS, ok ? "APPROVED" : "REJECTED", TIMELINE, List.of("approval: " + s.approval()));
    }

    public Map<String, Object> execute(IncidentState s) {
        ActionType action = ActionType.valueOf(s.actionType());
        String result = env.execute(s.service(), action);
        log(s, "execute", EXECUTED_PREFIX + action.name(), result);
        return Map.of(STATUS, "EXECUTED", TIMELINE, List.of("execute: " + result));
    }

    public Map<String, Object> verify(IncidentState s, int maxAttempts) {
        ServiceMetric m = metrics.findFirstByServiceNameOrderByIdDesc(s.service()).orElseThrow();
        boolean healthy = m.getCpuPercent() < HEALTHY_CPU && m.poolUsage() < HEALTHY_POOL;
        String status = healthy ? "RESOLVED" : s.attempt() >= maxAttempts ? "ESCALATED" : "RETRYING";
        log(s, "verify", "VERIFIED", IncidentTools.describe(m) + " -> " + status);
        return Map.of(RECOVERED, healthy, STATUS, status,
                TIMELINE, List.of("verify: " + IncidentTools.describe(m) + " -> " + status));
    }
}
