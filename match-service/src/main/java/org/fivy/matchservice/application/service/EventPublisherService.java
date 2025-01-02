package org.fivy.matchservice.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {
    private final KafkaTemplate<String, MatchEvent> kafkaTemplate;

    public void publishMatchEvent(MatchEvent event) {
        log.debug("Publishing match event: {}", event);

        kafkaTemplate.send("match-events", event.getMatchId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish match event: {}", event.getType(), ex);
                    } else {
                        log.debug("Successfully published match event: {} for match: {}",
                                event.getType(), event.getMatchId());
                    }
                });
    }
}
