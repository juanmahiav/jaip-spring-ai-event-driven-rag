package com.beanvisionary.ai.service;

import com.beanvisionary.common.ChatRequest;
import com.beanvisionary.common.ChatResponse;
import com.beanvisionary.common.KafkaTopics;
import com.beanvisionary.common.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiConsumer {

    private final ChatClient chat;
    private final VectorStore vectorStore;
    private final KafkaTemplate<String, ChatResponse> responseProducer;
    private final KafkaTemplate<String, Map<String, Object>> toolProducer;

    private static final ObjectMapper M = new ObjectMapper();

    public AiConsumer(
            ChatClient chat,
            VectorStore vectorStore,
            KafkaTemplate<String, ChatResponse> responseProducer,
            KafkaTemplate<String, Map<String, Object>> toolProducer
    ) {
        this.chat = chat;
        this.vectorStore = vectorStore;
        this.responseProducer = responseProducer;
        this.toolProducer = toolProducer;
    }

    @KafkaListener(topics = KafkaTopics.AI_REQUESTS, groupId = "ai-service")
    public void handle(@Payload ChatRequest req) {
        List<Document> docs = vectorStore.similaritySearch(req.query());

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String modelReply = chat
                .prompt()
                .system("""
            You are a helpful assistant. Use the CONTEXT when relevant.
            If an action is required (like web search or sending an email),
            reply ONLY with a JSON object of the form:
            {"tool":"<name>","args":{...}}
            CONTEXT:
            %s
            """.formatted(context))
                .user(req.query())
                .call()
                .content();

        ToolCall tool = maybeParseTool(modelReply);
        if (tool != null) {
            Map<String, Object> toolMsg = new HashMap<>();
            toolMsg.put("requestId", req.requestId());
            toolMsg.put("tool", tool.name());
            toolMsg.put("args", tool.args() != null ? tool.args() : Map.of());
            toolProducer.send(KafkaTopics.AI_TOOL_CALLS, toolMsg);
            return;
        }

        List<String> citations = docs.stream()
                .map(d -> {
                    String id = d.getId();
                    if (id != null && !id.isBlank()) return id;
                    Object mid = d.getMetadata() != null ? d.getMetadata().get("id") : null;
                    return mid instanceof String s ? s : null;
                })
                .filter(Objects::nonNull)
                .toList();

        ChatResponse response = new ChatResponse(
                req.requestId(),
                req.userId(),
                req.sessionId(),
                modelReply,
                List.of(),
                citations,
                Instant.now()
        );

        responseProducer.send(KafkaTopics.AI_RESPONSES, response);
    }

    private ToolCall maybeParseTool(String content) {
        try {
            String trimmed = content == null ? "" : content.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null;

            var node = M.readTree(trimmed);
            if (!node.isObject() || !node.hasNonNull("tool")) return null;

            String name = node.get("tool").asText();
            Map<String, Object> args = node.has("args") && node.get("args").isObject()
                    ? M.convertValue(node.get("args"), new TypeReference<Map<String, Object>>() {})
                    : Map.of();

            return new ToolCall(name, args);
        } catch (Exception ignore) {
            return null;
        }
    }
}
