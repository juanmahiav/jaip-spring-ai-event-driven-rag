package com.beanvisionary.ai.service;

import com.beanvisionary.common.ChatResponse;
import com.beanvisionary.common.ToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.beanvisionary.common.KafkaTopics.AI_RESPONSES;
import static com.beanvisionary.common.KafkaTopics.AI_TOOL_RESULTS;

@Component
public class ToolResultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ToolResultConsumer.class);
    private final ChatClient chat;
    private final KafkaTemplate<String, ChatResponse> producer;
    
    private final Map<String, Map<String, Object>> processedRequests = new ConcurrentHashMap<>();
    
    private final Map<String, Map<String, String>> requestContext = new ConcurrentHashMap<>();

    public ToolResultConsumer(ChatClient chat, KafkaTemplate<String, ChatResponse> producer) {
        this.chat = chat;
        this.producer = producer;
    }
    
    public void storeRequestContext(String requestId, String userId, String sessionId) {
        requestContext.put(requestId, Map.of("userId", userId, "sessionId", sessionId));
        logger.info("Stored context for request {}: userId={}, sessionId={}", requestId, userId, sessionId);
    }

    @KafkaListener(topics = AI_TOOL_RESULTS, groupId = "ai-service")
    public void handle(Map<String, Object> msg) {
        logger.info("Received tool result: {}", msg);
        
        try {
            if (msg == null) {
                logger.warn("Received null message");
                return;
            }
            
            String requestId = (String) msg.get("requestId");
            String toolName  = (String) msg.get("tool");
            Map<String, Object> args = (Map<String, Object>) msg.get("args");
            Map<String, Object> result = (Map<String, Object>) msg.get("result");
            
            if (requestId == null) {
                logger.warn("Received message with null requestId: {}", msg);
                return;
            }
            
            if (shouldProcessResult(requestId, result)) {
                logger.info("Processing tool result for request {} with tool {}", requestId, toolName);
                
                String resultJson = "null";
                try {
                    resultJson = new com.fasterxml.jackson.databind.ObjectMapper()
                            .valueToTree(result).toPrettyString();
                } catch (Exception e) {
                    logger.warn("Could not format result as JSON: {}", e.getMessage());
                }

                logger.info("Formatted tool result: {}", resultJson);

                String finalAnswer = generateDeterministicResponse(toolName, result);

                logger.info("Generated final answer for request {}: {}", requestId, finalAnswer);

                Map<String, String> context = requestContext.get(requestId);
                String userId = context != null ? context.get("userId") : "user-1";
                String sessionId = context != null ? context.get("sessionId") : "session-1";
                
                if (userId == null) userId = "user-1";
                if (sessionId == null) sessionId = "session-1";
                
                logger.info("Using context for response: userId={}, sessionId={}", userId, sessionId);

                ChatResponse response = new ChatResponse(requestId, userId, sessionId,
                        finalAnswer, List.of(new ToolCall(toolName, args != null ? args : Map.of())), List.of(), Instant.now());
                
                producer.send(AI_RESPONSES, response);
                logger.info("Sent final response to Kafka for request {}", requestId);
                
                requestContext.remove(requestId);
                
                processedRequests.put(requestId, result);
            } else {
                logger.info("Skipping tool result for request {} (already have better result)", requestId);
            }
            
        } catch (Exception e) {
            logger.error("Error processing tool result: {}", e.getMessage(), e);
        }
    }
    
    private boolean shouldProcessResult(String requestId, Map<String, Object> newResult) {
        Map<String, Object> existingResult = processedRequests.get(requestId);
        
        if (existingResult == null) {

            return true;
        }
        
        double existingScore = getScore(existingResult);
        double newScore = getScore(newResult);
        
        boolean shouldProcess = newScore > existingScore;
        logger.info("Comparing results for {}: existing score {} vs new score {} -> {}", 
            requestId, existingScore, newScore, shouldProcess ? "PROCESS" : "SKIP");
            
        return shouldProcess;
    }
    
    private double getScore(Map<String, Object> result) {
        Object score = result.get("score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        return 0.0;
    }
    
    private String generateDeterministicResponse(String toolName, Map<String, Object> result) {
        if (result == null) {
            return String.format("Tool %s executed but returned no result", toolName != null ? toolName : "unknown");
        }
        
        switch (toolName != null ? toolName : "") {
            case "checkSanctionsList":
                Boolean match = (Boolean) result.get("match");
                String rule = (String) result.get("rule");
                Object scoreObj = result.get("score");
                double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
                
                if (match != null && match) {
                    return String.format("**SANCTIONS MATCH FOUND**: Customer is on the sanctions list (Rule: %s, Confidence: %.0f%%). Transaction must be blocked and compliance team notified immediately.", 
                                       rule != null ? rule : "unknown", score * 100);
                } else {
                    return String.format("**NO SANCTIONS MATCH**: Customer is not on any sanctions list (Rule: %s, Confidence: %.0f%%). Transaction may proceed.", 
                                       rule != null ? rule : "unknown", score * 100);
                }
                
            case "lookupOrder":
                return String.format("Order lookup completed: %s", result.toString());
                
            case "launchCampaign":
                return String.format("Campaign launch completed: %s", result.toString());
                
            default:
                return String.format("Tool %s executed successfully: %s", toolName != null ? toolName : "unknown", result.toString());
        }
    }
}