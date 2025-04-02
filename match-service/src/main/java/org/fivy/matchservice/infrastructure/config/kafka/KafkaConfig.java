package org.fivy.matchservice.infrastructure.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.event.PushNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_MATCH_EVENTS = "match-events";
    public static final String TOPIC_PUSH_NOTIFICATIONS = "push-notifications";
    public static final String TOPIC_EMAIL_VERIFICATION = "email-verification-events";
    public static final String TOPIC_MATCH_VERIFICATION = "match-verification-events";
    public static final String TOPIC_MATCH_REMINDER = "match-reminder-events";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(JsonSerializer.TYPE_MAPPINGS,
                "notification:org.fivy.matchservice.domain.event.PushNotification," +
                        "event:org.fivy.matchservice.domain.event.MatchEvent," +
                        "emailVerification:org.fivy.matchservice.domain.event.email.EmailVerificationEvent," +
                        "matchVerification:org.fivy.matchservice.domain.event.email.MatchVerificationCodeEvent," +
                        "matchReminder:org.fivy.matchservice.domain.event.email.MatchReminderEvent");
        return props;
    }

    @Bean
    public ProducerFactory<String, MatchEvent> matchEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, MatchEvent> matchEventKafkaTemplate() {
        return new KafkaTemplate<>(matchEventProducerFactory());
    }

    @Bean
    @Primary
    public ProducerFactory<String, Object> genericProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public ProducerFactory<String, PushNotification> notificationProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, PushNotification> notificationKafkaTemplate() {
        return new KafkaTemplate<>(notificationProducerFactory());
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }

    @Bean
    public NewTopic matchEventsTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(TOPIC_MATCH_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pushNotificationsTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(TOPIC_PUSH_NOTIFICATIONS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailVerificationTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(TOPIC_EMAIL_VERIFICATION)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic matchVerificationTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(TOPIC_MATCH_VERIFICATION)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic matchReminderTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(TOPIC_MATCH_REMINDER)
                .partitions(3)
                .replicas(1)
                .build();
    }

}