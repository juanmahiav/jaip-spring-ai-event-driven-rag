package com.beanvisionary.tool;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static com.beanvisionary.common.KafkaTopics.AI_TOOL_CALLS;
import static com.beanvisionary.common.KafkaTopics.AI_TOOL_RESULTS;

@Service
public class ToolCallConsumer {
    private final KafkaTemplate<String, Map<String, Object>> producer;
    private final WebClient mcp = WebClient.builder().baseUrl("http://localhost:8091").build();

    public ToolCallConsumer(KafkaTemplate<String, Map<String, Object>> producer) {
        this.producer = producer;
    }

    @KafkaListener(topics = AI_TOOL_CALLS, groupId = "tool-service")
    public void handle(Map<String, Object> msg) {
        String requestId = (String) msg.get("requestId");
        String tool = (String) msg.get("tool");
        Map<String, Object> args = (Map<String, Object>) msg.get("args");

        Map<String, Object> safeArgs = args != null ? args : Map.of();

        Map<String, Object> result = mcp.post()
                .uri("/mcp/tools/{tool}", tool)
                .bodyValue(safeArgs)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        producer.send(AI_TOOL_RESULTS, Map.of(
            "requestId", requestId, 
            "tool", tool, 
            "args", safeArgs,
            "result", result
        ));
    }
}