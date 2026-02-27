package com.payment.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "notification.kafka.topics.auto-create", havingValue = "true")
public class KafkaTopicConfig {

    @Bean
    public NewTopic deadLetterQueueTopic() {
        return TopicBuilder.name("dead-letter-queue")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
