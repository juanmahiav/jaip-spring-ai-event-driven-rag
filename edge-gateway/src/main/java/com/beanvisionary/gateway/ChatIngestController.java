package com.beanvisionary.gateway;

import com.beanvisionary.common.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.beanvisionary.common.KafkaTopics.AI_REQUESTS;

@RestController
@CrossOrigin(origins = {"http://localhost:8000", "http://127.0.0.1:8000"}, allowedHeaders = {"Content-Type","Authorization","Accept"}, methods = {RequestMethod.POST, RequestMethod.OPTIONS})
@RequestMapping("/api/chat")
public class ChatIngestController {

    private static final Logger logger = LoggerFactory.getLogger(ChatIngestController.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ChatIngestController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public record IngestRequest(
            String requestId,
            String userId,
            String sessionId,
            String query,
            Map<String, Object> metadata
    ) {}

    public record IngestResponse(
            String requestId,
            String sessionId,
            String subscription,
            String status
    ) {}

    @PostMapping
    public IngestResponse ingest(@RequestBody IngestRequest body) {
        String requestId = body.requestId() != null && !body.requestId().isBlank() ? body.requestId() : UUID.randomUUID().toString();
        String sessionId = body.sessionId() != null && !body.sessionId().isBlank() ? body.sessionId() : "default";

        if (body.query() == null || body.query().isBlank()) {
            return new IngestResponse(requestId, sessionId, "/topic/replies." + sessionId, "REJECTED_EMPTY_QUERY");
        }

        ChatRequest event = new ChatRequest(
                requestId,
                body.userId(),
                sessionId,
                body.query(),
                body.metadata() != null ? body.metadata() : Map.of(),
                Instant.now()
        );

        logger.debug("Sending message to Kafka topic: {}", AI_REQUESTS);
        logger.debug("Message content: {}", event);

        try {
            kafkaTemplate.send(AI_REQUESTS, requestId, event);
            logger.debug("Message sent successfully");
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
        }

        return new IngestResponse(requestId, sessionId, "/topic/replies." + sessionId, "QUEUED");
    }
}