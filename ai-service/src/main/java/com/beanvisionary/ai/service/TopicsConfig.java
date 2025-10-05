package com.beanvisionary.ai.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class TopicsConfig {

    @Bean
    public NewTopic aiRequestsTopic() {
        return TopicBuilder.name("ai.requests.v1").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic aiResponsesTopic() {
        return TopicBuilder.name("ai.responses.v1").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic aiToolCallsTopic() {
        return TopicBuilder.name("ai.tool.calls.v1").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic aiToolResultsTopic() {
        return TopicBuilder.name("ai.tool.results.v1").partitions(1).replicas(1).build();
    }
}