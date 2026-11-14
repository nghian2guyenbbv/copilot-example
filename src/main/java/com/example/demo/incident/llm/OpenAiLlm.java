package com.example.demo.incident.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Any OpenAI-compatible /chat/completions endpoint, forced into JSON mode. */
@Component
@ConditionalOnExpression("!'${incident.llm.api-key:}'.isEmpty()")
public class OpenAiLlm implements LlmClient {

    private static final String SYSTEM = """
            You are an SRE incident-response agent. Using ONLY the data provided, choose ONE remediation.
            Reply with a single JSON object and nothing else, exactly:
            {"action_type": "SCALE_OUT|KILL_IDLE_SESSIONS|RESTART_SERVICE|NONE",
             "target_service": "<the service name>",
             "risk_level": "LOW|MEDIUM|HIGH"}
            Rules: SCALE_OUT is LOW risk. KILL_IDLE_SESSIONS and RESTART_SERVICE are HIGH risk.
            Do not repeat an action listed in actions_this_incident if it did not fix the problem.""";

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;

    public OpenAiLlm(@Value("${incident.llm.base-url}") String baseUrl,
                     @Value("${incident.llm.api-key}") String apiKey,
                     @Value("${incident.llm.model}") String model,
                     ObjectMapper mapper) {
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey).build();
        this.mapper = mapper;
        this.model = model;
    }

    @Override
    public String complete(DecisionContext ctx, String correction) {
        try {
            String user = mapper.writeValueAsString(ctx) + (correction == null ? "" : "\nPrevious answer was invalid: " + correction);
            JsonNode response = client.post().uri("/chat/completions")
                    .body(Map.of(
                            "model", model,
                            "response_format", Map.of("type", "json_object"),
                            "messages", List.of(
                                    Map.of("role", "system", "content", SYSTEM),
                                    Map.of("role", "user", "content", user))))
                    .retrieve().body(JsonNode.class);
            return response.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            throw new IllegalStateException("LLM call failed", e);
        }
    }
}
