package com.example.demo.incident.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "Runbooks", indexes = {
        @Index(name = "ix_runbooks_service", columnList = "service_name")
})
public class Runbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(nullable = false, length = 1000)
    private String instructions;

    protected Runbook() {
    }

    public Runbook(String serviceName, String actionType, String instructions) {
        this.serviceName = serviceName;
        this.actionType = actionType;
        this.instructions = instructions;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getInstructions() {
        return instructions;
    }
}
