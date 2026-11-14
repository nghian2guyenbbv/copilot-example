package com.example.demo.incident.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The strict JSON contract the LLM must return. */
public record Decision(
        @JsonProperty(value = "action_type", required = true) ActionType actionType,
        @JsonProperty(value = "target_service", required = true) String targetService,
        @JsonProperty(value = "risk_level", required = true) RiskLevel riskLevel) {
}
