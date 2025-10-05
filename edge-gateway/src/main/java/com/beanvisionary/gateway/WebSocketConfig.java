package com.beanvisionary.gateway;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*");

    registry.addEndpoint("/ws-sockjs")
        .setAllowedOriginPatterns("*")
        .withSockJS()
        .setSessionCookieNeeded(false);
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}