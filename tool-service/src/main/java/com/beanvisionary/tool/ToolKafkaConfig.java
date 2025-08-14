package com.beanvisionary.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class ToolKafkaConfig {

    @Bean
    RecordMessageConverter recordMessageConverter(ObjectMapper mapper) {
        return new StringJsonMessageConverter(mapper);
    }
}
