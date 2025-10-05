package com.beanvisionary.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(CustomOllamaService customOllamaService) {
        CustomChatModelAdapter chatModelAdapter = new CustomChatModelAdapter(customOllamaService);
        return ChatClient.builder(chatModelAdapter).build();
    }
}