package com.beanvisionary.ai.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

public class CustomChatModelAdapter implements ChatModel {
    
    private final CustomOllamaService customOllamaService;
    
    public CustomChatModelAdapter(CustomOllamaService customOllamaService) {
        this.customOllamaService = customOllamaService;
    }
    
    @Override
    public ChatResponse call(Prompt prompt) {
        return customOllamaService.call(prompt);
    }

    public CustomOllamaService getService() {
        return customOllamaService;
    }
}