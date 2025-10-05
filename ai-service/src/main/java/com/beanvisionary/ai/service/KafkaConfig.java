package com.beanvisionary.ai.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class KafkaConfig {

    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        objectMapper.registerModule(new JavaTimeModule());
        
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Instant.class, new UnixTimestampDeserializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }

    @Bean
    public RecordMessageConverter recordMessageConverter(ObjectMapper kafkaObjectMapper) {
        return new JsonMessageConverter(kafkaObjectMapper);
    }
    
    private static class UnixTimestampDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.getCurrentToken().isNumeric()) {

                double timestamp = p.getDoubleValue();
                long seconds = (long) timestamp;
                long nanos = (long) ((timestamp - seconds) * 1_000_000_000);
                return Instant.ofEpochSecond(seconds, nanos);
            }

            return Instant.parse(p.getValueAsString());
        }
    }
}