package com.example.demo.incident.graph;

import com.example.demo.incident.checkpoint.SqlServerCheckpointSaver;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.example.demo.incident.graph.IncidentState.APPROVED;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * START -> dispatch -+-> lookup_history --+
 *            ^       +-> lookup_runbooks -+-> decide(LLM) --HIGH risk--> approval_gate [INTERRUPT] --approved--> execute
 *            |       +-> scan_metrics ----+          \--------------------------------------------------------/    |
 *            |                                       rejected -> END                                              v
 *            +------------------(not recovered, attempts left)---------------------------------------------- verify -> END
 */
@Configuration
public class IncidentGraphConfig {

    public static final String APPROVAL_GATE = "approval_gate";

    @Bean
    public CompiledGraph<IncidentState> incidentGraph(IncidentNodes n, SqlServerCheckpointSaver saver,
                                                      @Value("${incident.max-attempts:3}") int maxAttempts)
            throws GraphStateException {
        return new StateGraph<>(IncidentState.SCHEMA, IncidentState::new)
                .addNode("dispatch", node_async(n::dispatch))
                .addNode("lookup_history", node_async(n::lookupHistory))
                .addNode("lookup_runbooks", node_async(n::lookupRunbooks))
                .addNode("scan_metrics", node_async(n::scanMetrics))
                .addNode("decide", node_async(n::decide))
                .addNode(APPROVAL_GATE, node_async(n::gate))
                .addNode("execute", node_async(n::execute))
                .addNode("verify", node_async(s -> n.verify(s, maxAttempts)))
                .addEdge(START, "dispatch")
                // fan-out: three branches run in parallel in the same superstep
                .addEdge("dispatch", "lookup_history")
                .addEdge("dispatch", "lookup_runbooks")
                .addEdge("dispatch", "scan_metrics")
                // fan-in
                .addEdge("lookup_history", "decide")
                .addEdge("lookup_runbooks", "decide")
                .addEdge("scan_metrics", "decide")
                .addConditionalEdges("decide",
                        edge_async(s -> n.needsApproval(s) ? "approve" : "run"),
                        Map.of("approve", APPROVAL_GATE, "run", "execute"))
                .addConditionalEdges(APPROVAL_GATE,
                        edge_async(s -> APPROVED.equals(s.approval()) ? "run" : "stop"),
                        Map.of("run", "execute", "stop", END))
                .addEdge("execute", "verify")
                .addConditionalEdges("verify",
                        edge_async(s -> s.recovered() || s.attempt() >= maxAttempts ? "done" : "retry"),
                        Map.of("done", END, "retry", "dispatch"))
                // The graph pauses before the gate; state is persisted in SQL Server so it survives restarts.
                .compile(CompileConfig.builder()
                        .checkpointSaver(saver)
                        .interruptBefore(APPROVAL_GATE)
                        .releaseThread(false)
                        .build());
    }
}
