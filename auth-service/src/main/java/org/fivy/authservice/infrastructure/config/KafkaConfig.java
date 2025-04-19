package org.fivy.authservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.fivy.authservice.domain.event.AuthEvent;
import org.fivy.authservice.domain.event.EmailVerificationEvent;
import org.fivy.authservice.domain.event.PasswordResetEvent;
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

    public static final String TOPIC_AUTH_EVENTS = "auth-events";
    public static final String TOPIC_EMAIL_VERIFICATION = "email-verification-events";
    public static final String TOPIC_PASSWORD_RESET = "password-reset-events";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(JsonSerializer.TYPE_MAPPINGS,
                "auth:org.fivy.authservice.domain.event.AuthEvent," +
                        "email:org.fivy.authservice.domain.event.EmailVerificationEvent," +
                        "passwordReset:org.fivy.authservice.domain.event.PasswordResetEvent");
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

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
        DefaultKafkaProducerFactory<String, AuthEvent> factory =
                new DefaultKafkaProducerFactory<>(
                        producerConfigs(),
                        new StringSerializer(),
                        new JsonSerializer<>(objectMapper())
                );
        return factory;
    }

    @Bean
    public KafkaTemplate<String, AuthEvent> kafkaTemplate() {
        return new KafkaTemplate<>(authEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, EmailVerificationEvent> emailVerificationEventProducerFactory() {
        DefaultKafkaProducerFactory<String, EmailVerificationEvent> factory =
                new DefaultKafkaProducerFactory<>(
                        producerConfigs(),
                        new StringSerializer(),
                        new JsonSerializer<>(objectMapper())
                );
        return factory;
    }

    @Bean
    public KafkaTemplate<String, EmailVerificationEvent> kafkaTemplateEmail() {
        return new KafkaTemplate<>(emailVerificationEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, PasswordResetEvent> passwordResetEventProducerFactory() {
        DefaultKafkaProducerFactory<String, PasswordResetEvent> factory =
                new DefaultKafkaProducerFactory<>(
                        producerConfigs(),
                        new StringSerializer(),
                        new JsonSerializer<>(objectMapper())
                );
        return factory;
    }

    @Bean
    public KafkaTemplate<String, PasswordResetEvent> kafkaTemplatePasswordReset() {
        return new KafkaTemplate<>(passwordResetEventProducerFactory());
    }

    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder.name(TOPIC_AUTH_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailVerificationTopic() {
        return TopicBuilder.name(TOPIC_EMAIL_VERIFICATION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic passwordResetTopic() {
        return TopicBuilder.name(TOPIC_PASSWORD_RESET)
                .partitions(3)
                .replicas(1)
                .build();
    }
}