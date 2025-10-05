package com.beanvisionary.ai.service;

import com.beanvisionary.common.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class CustomOllamaService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOllamaService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String modelName;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ToolResultConsumer toolResultConsumer;

    public CustomOllamaService(
            @Value("${spring.ai.ollama.base-url}") String baseUrl,
            @Value("${spring.ai.ollama.chat.options.model}") String modelName,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Lazy ToolResultConsumer toolResultConsumer) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.kafkaTemplate = kafkaTemplate;
        this.toolResultConsumer = toolResultConsumer;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ChatResponse call(Prompt prompt) {
        try {
            List<Map<String, Object>> ollamaMessages = toOllamaMessages(prompt);
            Map<String, Object> requestBody = Map.of(
                    "model", modelName,
                    "messages", ollamaMessages,
                    "stream", false
            );

            String response = restClient.post()
                    .uri(UriComponentsBuilder.fromHttpUrl(baseUrl).path("/api/chat").build().toUri())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(response);
            String content = json.path("message").path("content").asText("");
            AssistantMessage assistantMessage = new AssistantMessage(content);
            Generation generation = new Generation(assistantMessage);
            try {
                return new ChatResponse(List.of(generation));
            } catch (NoSuchMethodError ignored) {
                return new ChatResponse(List.of(generation), null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error calling Ollama API: " + e.getMessage(), e);
        }
    }

    public void stream(Prompt prompt,
                       String requestId,
                       String userId,
                       String sessionId,
                       Consumer<com.beanvisionary.common.ChatResponse> callback) {

        toolResultConsumer.storeRequestContext(requestId, userId, sessionId);

        List<Map<String, Object>> ollamaMessages = toOllamaMessages(prompt);
        
        List<Map<String, Object>> tools = getAvailableTools();
        
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", ollamaMessages,
                "stream", true,
                "tools", tools
        );

        StringBuilder responseContent = new StringBuilder();
        List<ToolCall> detectedToolCalls = new ArrayList<>();
        
        try {
            restClient.post().uri(UriComponentsBuilder.fromHttpUrl(baseUrl).path("/api/chat").build().toUri()).body(requestBody).exchange((req, resp) -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        JsonNode node;
                        try {
                            node = objectMapper.readTree(line);
                        } catch (Exception parseEx) {
                            continue;
                        }
                        boolean done = node.path("done").asBoolean(false);
                        String thinking = node.path("thinking").asText(null);
                        if (thinking != null && !thinking.isBlank()) {
                            callback.accept(new com.beanvisionary.common.ChatResponse(
                                    requestId, userId, sessionId, "[partial][thinking] " + thinking,
                                    List.of(), List.of(), Instant.now()
                            ));
                        }
                        
                        JsonNode messageNode = node.path("message");
                        if (messageNode.has("tool_calls")) {
                            JsonNode toolCallsNode = messageNode.path("tool_calls");
                            if (toolCallsNode.isArray()) {
                                for (JsonNode toolCallNode : toolCallsNode) {
                                    String toolName = toolCallNode.path("function").path("name").asText();
                                    String argumentsJson = toolCallNode.path("function").path("arguments").asText();
                                    try {
                                        Map<String, Object> arguments;
                                        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
                                            arguments = Map.of();
                                        } else {
                                            arguments = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
                                        }
                                        ToolCall toolCall = new ToolCall(toolName, arguments);
                                        detectedToolCalls.add(toolCall);
                                        logger.info("Detected tool call from Ollama: {} with args: {}", toolName, arguments);
                                        
                                    } catch (Exception e) {
                                        logger.error("Error parsing tool call arguments for {}: {}", toolName, e.getMessage());
                                    }
                                }
                            }
                        }
                        
                        String fragment = messageNode.path("content").asText("");
                        if (!fragment.isEmpty()) {
                            appendContentFragment(responseContent, fragment);
                        }
                        
                        if (done) {

                            ToolCall bestToolCall = null;

                            if (!detectedToolCalls.isEmpty()) {
                                bestToolCall = detectedToolCalls.get(0);
                                if (bestToolCall != null) {
                                    logger.info("Using Ollama-detected tool call: {} with args: {}", bestToolCall.name(), bestToolCall.args());
                                }
                            }

                            String finalContent = responseContent.toString();

                            if (bestToolCall != null && (bestToolCall.args() == null || bestToolCall.args().isEmpty())) {
                                logger.info("Ollama tool call has empty args, trying fallback methods");
                                bestToolCall = findFallbackToolCall(finalContent, ollamaMessages);
                            }
                            
                            if (bestToolCall == null) {
                                bestToolCall = findFallbackToolCall(finalContent, ollamaMessages);
                            }

                            if (bestToolCall != null) {
                                detectedToolCalls.clear();
                                detectedToolCalls.add(bestToolCall);
                                
                                Map<String, Object> toolCallMessage = Map.of(
                                        "requestId", requestId,
                                        "tool", bestToolCall.name(),
                                        "args", bestToolCall.args()
                                );
                                kafkaTemplate.send("ai.tool.calls.v1", requestId, toolCallMessage);
                                logger.info("Sent SINGLE tool call to Kafka: {} for request {} with args: {}", bestToolCall.name(), requestId, bestToolCall.args());
                            }
                            
                            if (detectedToolCalls.isEmpty()) {

                                callback.accept(new com.beanvisionary.common.ChatResponse(
                                        requestId, userId, sessionId, finalContent,
                                        List.of(), List.of(), Instant.now()
                                ));
                            }
                            break;
                        } else if (responseContent.length() > 0) {

                            String partialContent = responseContent.toString();
                            callback.accept(new com.beanvisionary.common.ChatResponse(
                                    requestId, userId, sessionId, "[partial] " + partialContent,
                                    detectedToolCalls, List.of(), Instant.now()
                            ));
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Error in streaming call", e);
            callback.accept(new com.beanvisionary.common.ChatResponse(
                    requestId, userId, sessionId,
                    "Error (stream) calling Ollama: " + e.getMessage(), List.of(), List.of(), Instant.now()
            ));
        }
    }

    private List<Map<String, Object>> toOllamaMessages(Prompt prompt) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Message m : prompt.getInstructions()) {
            if (m instanceof UserMessage) {
                list.add(Map.of("role", "user", "content", m.getText()));
            } else if (m instanceof AssistantMessage) {
                list.add(Map.of("role", "assistant", "content", m.getText()));
            } else if (m instanceof SystemMessage) {
                list.add(Map.of("role", "system", "content", m.getText()));
            }
        }
        return list;
    }

    private List<Map<String, Object>> getAvailableTools() {
        return List.of(
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "checkSanctionsList",
                                "description", "Check if a person is on the sanctions list",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "name", Map.of(
                                                        "type", "string",
                                                        "description", "The full name of the person to check"
                                                )
                                        ),
                                        "required", List.of("name")
                                )
                        )
                ),
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "lookupOrder",
                                "description", "Look up order details by order ID",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "orderId", Map.of(
                                                        "type", "string",
                                                        "description", "The order ID to look up"
                                                )
                                        ),
                                        "required", List.of("orderId")
                                )
                        )
                ),
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "launchCampaign",
                                "description", "Launch a marketing campaign",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "budget", Map.of(
                                                        "type", "number",
                                                        "description", "Campaign budget in dollars"
                                                ),
                                                "channel", Map.of(
                                                        "type", "string",
                                                        "description", "Marketing channel (e.g., LinkedIn, Facebook)"
                                                ),
                                                "audienceId", Map.of(
                                                        "type", "number",
                                                        "description", "Target audience ID"
                                                ),
                                                "creative", Map.of(
                                                        "type", "string",
                                                        "description", "Creative asset identifier"
                                                )
                                        ),
                                        "required", List.of("budget")
                                )
                        )
                )
        );
    }

    private String extractUserQuery(List<Map<String, Object>> messages) {
        return messages.stream()
                .filter(msg -> "user".equals(msg.get("role")))
                .map(msg -> (String) msg.get("content"))
                .reduce((first, second) -> second)
                .orElse("");
    }

    private ToolCall suggestToolForQuery(String query) {
        if (query == null) return null;
        String lowerQuery = query.toLowerCase();
        
        if ((lowerQuery.contains("sanction") || lowerQuery.contains("check")) && 
            (lowerQuery.contains("name") || lowerQuery.contains("person") || lowerQuery.contains("customer"))) {

            String name = extractNameFromQuery(query);
            if (name != null) {
                return new ToolCall("checkSanctionsList", Map.of("name", name));
            }
        }
        
        if ((lowerQuery.contains("order") || lowerQuery.contains("status")) && 
            (lowerQuery.contains("look") || lowerQuery.contains("check") || lowerQuery.contains("find"))) {
            String orderId = extractOrderIdFromQuery(query);
            if (orderId != null) {
                return new ToolCall("lookupOrder", Map.of("orderId", orderId));
            }
        }
        
        if (lowerQuery.contains("campaign") || lowerQuery.contains("launch") || lowerQuery.contains("marketing")) {

            return new ToolCall("launchCampaign", Map.of("budget", 500));
        }
        
        return null;
    }

    private String extractNameFromQuery(String query) {

        String[] patterns = {
                "customer\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
                "person\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
                "([A-Z][a-z]+\\s+[A-Z][a-z]+)\\s+is",
                "\\b([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(query);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private String extractOrderIdFromQuery(String query) {

        String[] patterns = {
                "order\\s+([A-Z]?\\d+[A-Z]*)",
                "order\\s+ID\\s+([A-Z]?\\d+[A-Z]*)",
                "\\b([A-Z]\\d{4,})\\b"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(query);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * Appends a content fragment to the response builder with intelligent deduplication.
     * 
     * This method handles three scenarios:
     * 1. Empty response: Simply append the fragment
     * 2. Fragment contains current response as prefix: Append only the new part
     * 3. Fragment is not a duplicate suffix: Append the entire fragment
     * 
     * The deduplication prevents streaming responses from showing repeated content
     * when the AI model sends overlapping text fragments.
     * 
     * @param responseContent The StringBuilder containing the accumulated response
     * @param fragment The new content fragment to potentially append
     */
    private void appendContentFragment(StringBuilder responseContent, String fragment) {
        int currentLength = responseContent.length();
        
        if (currentLength == 0) {

            responseContent.append(fragment);
        } else {

            if (fragment.length() >= currentLength &&
                    fragment.regionMatches(0, responseContent, 0, currentLength)) {

                if (fragment.length() > currentLength) {
                    responseContent.append(fragment.substring(currentLength));
                }
            } else {

                boolean isDuplicateSuffix = currentLength >= fragment.length() &&
                        responseContent.subSequence(currentLength - fragment.length(), currentLength).equals(fragment);
                
                if (!isDuplicateSuffix) {

                    responseContent.append(fragment);
                }
            }
        }
    }

    /**
     * Attempts to find a tool call using fallback methods when primary detection fails.
     * 
     * This method tries two approaches in sequence:
     * 1. Extract tool calls from the response content using ToolParsing
     * 2. Suggest a tool based on the user query analysis
     * 
     * @param finalContent The final response content to parse for tool calls
     * @param ollamaMessages The conversation messages to extract user query from
     * @return A ToolCall if found, null otherwise
     */
    private ToolCall findFallbackToolCall(String finalContent, List<Map<String, Object>> ollamaMessages) {

        List<ToolCall> extractedToolCalls = ToolParsing.extractToolCalls(finalContent);
        if (!extractedToolCalls.isEmpty()) {
            ToolCall toolCall = extractedToolCalls.get(0);
            logger.info("Using extracted tool call: {} with args: {}", toolCall.name(), toolCall.args());
            return toolCall;
        }

        String query = extractUserQuery(ollamaMessages);
        ToolCall suggestedTool = suggestToolForQuery(query);
        if (suggestedTool != null) {
            logger.info("Using suggested tool call: {} with args: {}", suggestedTool.name(), suggestedTool.args());
            return suggestedTool;
        }
        
        return null;
    }
}