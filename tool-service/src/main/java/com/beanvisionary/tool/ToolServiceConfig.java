package com.beanvisionary.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ToolServiceConfig {
    @Bean
    public WebClient mcp(@Value("${mcp.base-url:http://localhost:8091}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
