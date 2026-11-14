package com.example.demo.incident.llm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/** Calls the LLM and enforces the strict JSON contract, retrying once with the validation error. */
@Service
public class DecisionService {

    private final LlmClient llm;
    private final ObjectMapper strict = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public DecisionService(LlmClient llm) {
        this.llm = llm;
    }

    public Decision decide(DecisionContext ctx) {
        String error = null;
        for (int i = 0; i < 2; i++) {
            String raw = llm.complete(ctx, error);
            try {
                Decision d = strict.readValue(raw, Decision.class);
                if (d.actionType() == null || d.riskLevel() == null || !ctx.service().equals(d.targetService())) {
                    throw new IllegalArgumentException("missing field or wrong target_service");
                }
                return d;
            } catch (Exception e) {
                error = e.getMessage();
            }
        }
        throw new IllegalStateException("LLM returned invalid decision: " + error);
    }
}
