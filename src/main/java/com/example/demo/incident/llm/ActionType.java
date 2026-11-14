package com.example.demo.incident.llm;

public enum ActionType {
    SCALE_OUT("kubectl scale deployment/%s --replicas=4"),
    KILL_IDLE_SESSIONS("psql -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state='idle in transaction'\" # %s"),
    RESTART_SERVICE("kubectl rollout restart deployment/%s"),
    NONE("# no action for %s");

    private final String commandTemplate;

    ActionType(String commandTemplate) {
        this.commandTemplate = commandTemplate;
    }

    public String command(String service) {
        return commandTemplate.formatted(service);
    }
}
