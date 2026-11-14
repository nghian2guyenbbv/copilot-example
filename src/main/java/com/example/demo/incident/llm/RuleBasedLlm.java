package com.example.demo.incident.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Offline stand-in for an LLM so the project runs without an API key. */
@Component
@ConditionalOnExpression("'${incident.llm.api-key:}'.isEmpty()")
public class RuleBasedLlm implements LlmClient {

    @Override
    public String complete(DecisionContext ctx, String correction) {
        ActionType action;
        RiskLevel risk;
        if (ctx.cpuHigh()) {
            action = ActionType.SCALE_OUT;
            risk = RiskLevel.LOW;
        } else if (ctx.poolHigh()) {
            risk = RiskLevel.HIGH;
            boolean alreadyKilled = ctx.actionsThisIncident().contains(ActionType.KILL_IDLE_SESSIONS.name());
            action = alreadyKilled ? ActionType.RESTART_SERVICE : ActionType.KILL_IDLE_SESSIONS;
        } else {
            action = ActionType.NONE;
            risk = RiskLevel.LOW;
        }
        return """
                {"action_type":"%s","target_service":"%s","risk_level":"%s"}"""
                .formatted(action, ctx.service(), risk);
    }
}
