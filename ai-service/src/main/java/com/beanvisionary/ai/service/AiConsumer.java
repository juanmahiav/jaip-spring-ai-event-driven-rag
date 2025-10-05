package com.beanvisionary.ai.service;

import com.beanvisionary.common.ChatResponse;
import com.beanvisionary.common.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AiConsumer.class);

    private final VectorStore vectorStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String defaultTopic;
    private final CustomOllamaService customOllamaService;

    public AiConsumer(
            VectorStore vectorStore,
            KafkaTemplate<String, Object> kafkaTemplate,
            CustomOllamaService customOllamaService,
            ObjectMapper objectMapper,
            @Value("${spring.kafka.template.default-topic}") String defaultTopic) {
        this.vectorStore = vectorStore;
        this.kafkaTemplate = kafkaTemplate;
        this.customOllamaService = customOllamaService;
        this.defaultTopic = defaultTopic;
        this.objectMapper = objectMapper;
    }
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ai.requests.v1", groupId = "ai-service")
    public void handle(ChatRequest chatRequest) {
        logger.info("Received message from Kafka, processing request: {}", chatRequest.requestId());

        try {
            
            logger.info("Searching vector store for query: {}", chatRequest.query());
            List<Document> similarDocuments = vectorStore.similaritySearch(chatRequest.query());
            logger.info("Found {} similar documents", similarDocuments.size());

            String context = similarDocuments.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
            logger.info("Formatted context from documents");

            try {

                String systemPrompt = buildSystemPrompt(context);

                Prompt prompt = new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(chatRequest.query())
                ));

                final String rid = chatRequest.requestId();
                final String uid = chatRequest.userId();
                final String sid = chatRequest.sessionId();
                logger.info("Starting streaming response for request {}", rid);
                customOllamaService.stream(prompt, rid, uid, sid, partial -> {

                    logger.info("Sending partial response for request {}: {}", rid, partial);
                    kafkaTemplate.send(defaultTopic, rid, partial);
                });

            } catch (Exception e) {
                logger.error("Error calling Ollama API: {}", e.getMessage(), e);

                ChatResponse errorResponse = new ChatResponse(
                        chatRequest.requestId(),
                        chatRequest.userId(),
                        chatRequest.sessionId(),
                        "Error processing request: " + e.getMessage(),
                        List.of(),
                        List.of(),
                        Instant.now()
                );
                kafkaTemplate.send(defaultTopic, chatRequest.requestId(), errorResponse);
            }

        } catch (Exception e) {
            logger.error("Error processing request: {}", chatRequest.requestId(), e);

            ChatResponse errorResponse = new ChatResponse(
                    chatRequest.requestId(),
                    chatRequest.userId(),
                    chatRequest.sessionId(),
                    "Error processing request: " + e.getMessage(),
                    List.of(),
                    List.of(),
                    Instant.now()
            );
            kafkaTemplate.send(defaultTopic, chatRequest.requestId(), errorResponse);
        }
    }

    private String normalizeJson(String input) {
    if (input == null) return "";
    String cleaned = input
        .replace('\uFEFF', ' ')
        .replaceAll("\\p{C}", " ")
        .trim();
    if (cleaned.startsWith(">")) cleaned = cleaned.substring(1).trim();
    cleaned = cleaned
        .replace('“', '"')
        .replace('”', '"')
        .replace('‘', '"')
        .replace('’', '"');
    return cleaned;
    }

    private String extractField(String raw, String field) {
        if (raw == null) return null;
        try {
            Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
            Matcher m = p.matcher(raw);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) { }
        return null;
    }

    private String removeFieldUnsafe(String json, String field) {

        String regex = "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(\\\"[^\\\"]*\\\"|[0-9.]+)\\s*,?";
        return json.replaceAll(regex, "");
    }

    private String buildSystemPrompt(String context) {
        return """
                You are a helpful AI assistant with access to various tools. Use the following context to answer questions when relevant:
                
                CONTEXT:
                %s
                
                AVAILABLE TOOLS:
                1. checkSanctionsList - Use this when asked to check if a person is on a sanctions list
                2. lookupOrder - Use this when asked about order status, order details, or order information
                3. launchCampaign - Use this when asked to create or launch marketing campaigns
                
                GUIDELINES:
                - If the user asks about checking someone against a sanctions list, use the checkSanctionsList tool
                - If the user asks about order status or order details, use the lookupOrder tool
                - If the user asks about launching campaigns or marketing, use the launchCampaign tool
                - If you need to use a tool, call it with the appropriate parameters
                - If the question can be answered from the context without tools, answer directly
                - Be helpful and provide clear, accurate responses
                """.formatted(context);
    }
}