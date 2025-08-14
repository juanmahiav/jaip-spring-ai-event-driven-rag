package com.beanvisionary.ai.service;

import com.beanvisionary.common.ChatResponse;
import com.beanvisionary.common.ToolCall;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.beanvisionary.common.KafkaTopics.AI_RESPONSES;
import static com.beanvisionary.common.KafkaTopics.AI_TOOL_RESULTS;

@Component
@RequiredArgsConstructor
public class ToolResultConsumer {
    private final ChatClient chat;
    private final KafkaTemplate<String, ChatResponse> producer;

    @KafkaListener(topics = AI_TOOL_RESULTS, groupId = "ai-service")
    public void handle(Map<String, Object> msg) {
        String requestId = (String) msg.get("requestId");
        String toolName  = (String) msg.get("tool");
        String resultJson = new com.fasterxml.jackson.databind.ObjectMapper()
                .valueToTree(msg.get("result")).toPrettyString();

        var finalAnswer = chat
                .prompt()
                .system("Tool %s executed. Use RESULT to answer the user succinctly.".formatted(toolName))
                .user(resultJson)
                .call()
                .content();

        producer.send(AI_RESPONSES, new ChatResponse(requestId, null, null,
                finalAnswer, List.of(new ToolCall(toolName, Map.of())), List.of(), Instant.now()));
    }
}
