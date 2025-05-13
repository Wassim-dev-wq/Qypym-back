package org.fivy.userservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.fivy.userservice.domain.event.authEvent.AuthEvent;
import org.fivy.userservice.domain.event.userEvent.UserEvent;
import org.fivy.userservice.domain.event.email.MatchVerificationCodeEvent;
import org.fivy.userservice.domain.event.email.MatchReminderEvent;
import org.fivy.userservice.domain.event.email.MatchEmailEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_MATCH_EMAILS = "match-email-events";
    public static final String TOPIC_MATCH_VERIFICATION = "match-verification-events";
    public static final String TOPIC_MATCH_REMINDER = "match-reminder-events";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(JsonSerializer.TYPE_MAPPINGS,
                "userEvent:org.fivy.userservice.domain.event.userEvent.UserEvent," +
                        "authEvent:org.fivy.userservice.domain.event.authEvent.AuthEvent," +
                        "matchEmailEvent:org.fivy.userservice.domain.event.email.MatchEmailEvent," +
                        "matchVerification:org.fivy.userservice.domain.event.email.MatchVerificationCodeEvent," +
                        "matchReminder:org.fivy.userservice.domain.event.email.MatchReminderEvent");
        return props;
    }

    @Bean
    public ProducerFactory<String, UserEvent> userEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, UserEvent> userEventKafkaTemplate() {
        return new KafkaTemplate<>(userEventProducerFactory());
    }

    @Bean
    @Primary
    public ProducerFactory<String, Object> genericProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    @Bean
    public ConsumerFactory<String, AuthEvent> authEventConsumerFactory() {
        JsonDeserializer<AuthEvent> deserializer = new JsonDeserializer<>(AuthEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuthEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AuthEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(authEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, MatchVerificationCodeEvent> matchVerificationConsumerFactory() {
        JsonDeserializer<MatchVerificationCodeEvent> deserializer =
                new JsonDeserializer<>(MatchVerificationCodeEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchVerificationCodeEvent> matchVerificationListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MatchVerificationCodeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(matchVerificationConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, MatchReminderEvent> matchReminderConsumerFactory() {
        JsonDeserializer<MatchReminderEvent> deserializer =
                new JsonDeserializer<>(MatchReminderEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchReminderEvent> matchReminderListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MatchReminderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(matchReminderConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name("user-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder.name("auth-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic matchEmailsTopic() {
        return TopicBuilder.name(TOPIC_MATCH_EMAILS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}