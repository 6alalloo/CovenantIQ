package com.covenantiq.service;

import com.covenantiq.domain.EventOutbox;
import com.covenantiq.repository.EventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(EventOutboxRepository eventOutboxRepository, ObjectMapper objectMapper) {
        this.eventOutboxRepository = eventOutboxRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(String aggregateType, Long aggregateId, String eventType, Map<String, Object> payload) {
        try {
            EventOutbox outbox = new EventOutbox();
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> payloadCopy = new java.util.HashMap<>(payload);
            payloadCopy.putIfAbsent("eventVersion", 1);
            payloadCopy.putIfAbsent("eventId", eventId);
            outbox.setEventId(eventId);
            outbox.setAggregateType(aggregateType);
            outbox.setAggregateId(aggregateId);
            outbox.setEventType(eventType);
            outbox.setPayloadJson(objectMapper.writeValueAsString(payloadCopy));
            outbox.setHeadersJson("{\"eventVersion\":1}");
            eventOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload: eventType={}, aggregateType={}, aggregateId={}",
                    eventType, aggregateType, aggregateId, e);
            throw new IllegalStateException("Unable to serialize outbox payload");
        }
    }
}
