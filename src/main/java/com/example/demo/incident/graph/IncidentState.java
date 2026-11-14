package com.example.demo.incident.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IncidentState extends AgentState {

    public static final String INCIDENT_ID = "incidentId";
    public static final String SERVICE = "service";
    public static final String ALERT = "alert";
    public static final String FINDINGS = "findings";
    public static final String HISTORY = "history";
    public static final String RUNBOOKS = "runbooks";
    public static final String ACTION_TYPE = "actionType";
    public static final String RISK_LEVEL = "riskLevel";
    public static final String PLAN = "plan";
    public static final String APPROVAL = "approval";
    public static final String ATTEMPT = "attempt";
    public static final String RECOVERED = "recovered";
    public static final String STATUS = "status";
    public static final String TIMELINE = "timeline";

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(INCIDENT_ID, Channels.base(() -> "")),
            Map.entry(SERVICE, Channels.base(() -> "")),
            Map.entry(ALERT, Channels.base(() -> "")),
            Map.entry(FINDINGS, Channels.base(() -> List.<String>of())),
            Map.entry(HISTORY, Channels.base(() -> List.<String>of())),
            Map.entry(RUNBOOKS, Channels.base(() -> List.<String>of())),
            Map.entry(ACTION_TYPE, Channels.base(() -> "NONE")),
            Map.entry(RISK_LEVEL, Channels.base(() -> "LOW")),
            Map.entry(PLAN, Channels.base(() -> List.<RemediationStep>of())),
            Map.entry(APPROVAL, Channels.base(() -> PENDING)),
            Map.entry(ATTEMPT, Channels.base(() -> 0)),
            Map.entry(RECOVERED, Channels.base(() -> false)),
            Map.entry(STATUS, Channels.base(() -> "NEW")),
            Map.entry(TIMELINE, Channels.appender(ArrayList::new))
    );

    public IncidentState(Map<String, Object> initData) {
        super(initData);
    }

    public String incidentId() {
        return this.<String>value(INCIDENT_ID).orElse("");
    }

    public String service() {
        return this.<String>value(SERVICE).orElse("");
    }

    public String alert() {
        return this.<String>value(ALERT).orElse("");
    }

    public List<String> findings() {
        return this.<List<String>>value(FINDINGS).orElse(List.of());
    }

    public List<String> history() {
        return this.<List<String>>value(HISTORY).orElse(List.of());
    }

    public List<String> runbooks() {
        return this.<List<String>>value(RUNBOOKS).orElse(List.of());
    }

    public List<RemediationStep> plan() {
        return this.<List<RemediationStep>>value(PLAN).orElse(List.of());
    }

    public String actionType() {
        return this.<String>value(ACTION_TYPE).orElse("NONE");
    }

    public String riskLevel() {
        return this.<String>value(RISK_LEVEL).orElse("LOW");
    }

    public String approval() {
        return this.<String>value(APPROVAL).orElse(PENDING);
    }

    public int attempt() {
        return this.<Integer>value(ATTEMPT).orElse(0);
    }

    public boolean recovered() {
        return this.<Boolean>value(RECOVERED).orElse(false);
    }

    public String status() {
        return this.<String>value(STATUS).orElse("NEW");
    }

    public List<String> timeline() {
        return this.<List<String>>value(TIMELINE).orElse(List.of());
    }
}
