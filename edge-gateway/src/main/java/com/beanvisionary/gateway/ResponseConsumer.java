package com.beanvisionary.gateway;

import com.beanvisionary.common.ChatResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.beanvisionary.common.KafkaTopics.AI_RESPONSES;

@Service
public class ResponseConsumer {
    private final SimpMessagingTemplate ws;

    public ResponseConsumer(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    @KafkaListener(topics = AI_RESPONSES, groupId = "edge-gateway")
    public void forward(ChatResponse resp) {
        String dest = "/topic/replies." + (resp.sessionId() != null ? resp.sessionId() : "default");
        ws.convertAndSend(dest, resp);
    }
}