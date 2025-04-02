package org.fivy.authservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.fivy.authservice.domain.event.AuthEvent;
import org.fivy.authservice.domain.event.EmailVerificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(JsonSerializer.TYPE_MAPPINGS,
                "auth:org.fivy.authservice.domain.event.AuthEvent," +
                        "email:org.fivy.authservice.domain.event.EmailVerificationEvent");

        return props;
    }

    @Bean
    public ProducerFactory<String, Object> genericProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, Object> genericKafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }

    @Bean
    public ProducerFactory<String, AuthEvent> authEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, AuthEvent> kafkaTemplate() {
        return new KafkaTemplate<>(authEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, EmailVerificationEvent> emailVerificationEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, EmailVerificationEvent> kafkaTemplateEmail() {
        return new KafkaTemplate<>(emailVerificationEventProducerFactory());
    }

    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder.name("auth-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailVerificationTopic() {
        return TopicBuilder.name("email-verification-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}