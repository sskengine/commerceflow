package com.sskengine.commerceflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(KafkaTemplate.class)
    DomainEventPublisher domainEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        return new DomainEventPublisher(kafkaTemplate, objectMapper);
    }
}
