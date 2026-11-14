package com.example.demo.incident.llm;

/** Returns the raw JSON answer for a {@link DecisionContext}. */
public interface LlmClient {
    String complete(DecisionContext context, String correction);
}
