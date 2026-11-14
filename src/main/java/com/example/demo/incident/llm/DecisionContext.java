package com.example.demo.incident.llm;

import java.util.List;

/** Everything the LLM is allowed to see, all of it read from SQL Server by the tools. */
public record DecisionContext(
        String service,
        String alertMessage,
        List<String> activeIncidents,
        List<String> recentMetrics,
        List<String> actionsThisIncident,
        List<String> runbooks,
        boolean cpuHigh,
        boolean poolHigh) {
}
