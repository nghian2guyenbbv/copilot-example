package com.example.demo;

import com.example.demo.incident.IncidentService;
import com.example.demo.incident.domain.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests: need SQL Server from `docker compose up -d`. */
@SpringBootTest
class IncidentFlowTest {

    private static final String MESSAGE = "High CPU and connection pool exhaustion on payment-service";

    @Autowired
    IncidentService service;
    @Autowired
    AuditLogRepository audit;
    @Autowired
    JdbcTemplate jdbc;

    private static String uniqueService() {
        return "payment-service-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void loopsWithHumanApprovalUntilRecovered() throws Exception {
        var v = service.open(uniqueService(), MESSAGE);
        // Attempt 1 (scale out, LOW risk) ran automatically; attempt 2 (kill sessions) now waits for approval.
        assertTrue(v.awaitingApproval());
        assertEquals("KILL_IDLE_SESSIONS", v.plan().get(0).reason());
        assertTrue(v.plan().get(0).dangerous());
        assertTrue(v.history().isEmpty());

        // Checkpoint for the paused thread is persisted in SQL Server.
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM lg_checkpoints WHERE thread_id = ?", Integer.class, v.incidentId()) > 0);

        v = service.decide(v.incidentId(), true);   // leak refills the pool -> asks again
        assertTrue(v.awaitingApproval());
        assertEquals("RESTART_SERVICE", v.plan().get(0).reason());

        v = service.decide(v.incidentId(), true);
        assertEquals("RESOLVED", v.status());
        assertFalse(v.awaitingApproval());

        var actions = service.auditTrail(v.incidentId()).stream().map(a -> a.getAction()).toList();
        assertTrue(actions.containsAll(java.util.List.of(
                "EXECUTED_SCALE_OUT", "EXECUTED_KILL_IDLE_SESSIONS", "EXECUTED_RESTART_SERVICE")));
    }

    @Test
    void rejectionStopsWithoutRunningDangerousCommand() throws Exception {
        String id = service.open(uniqueService(), MESSAGE).incidentId();
        var v = service.decide(id, false);
        assertEquals("REJECTED", v.status());
        assertTrue(service.auditTrail(id).stream().noneMatch(a -> a.getAction().equals("EXECUTED_KILL_IDLE_SESSIONS")));
        assertThrows(IllegalStateException.class, () -> service.decide(id, true));
    }

    @Test
    void parallelInvestigationBranchesAllWriteAuditRows() {
        String id = service.open(uniqueService(), MESSAGE).incidentId();
        var nodes = service.auditTrail(id).stream().map(a -> a.getNode()).toList();
        assertTrue(nodes.containsAll(java.util.List.of("lookup_history", "lookup_runbooks", "scan_metrics")));
    }
}
