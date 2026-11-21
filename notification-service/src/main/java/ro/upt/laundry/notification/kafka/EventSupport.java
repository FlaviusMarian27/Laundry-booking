package ro.upt.laundry.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;

/**
 * Shared parsing logic for all listeners.
 * Stateless (except the start time), so it is safe to use from several consumer threads.
 */
@Component
public class EventSupport {

    private static final Logger log = LoggerFactory.getLogger(EventSupport.class);

    private final ObjectMapper mapper;
    private final Instant serviceStartedAt = Instant.now();

    public EventSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Parses the envelope; invalid messages are logged and skipped (empty result). */
    public Optional<EventEnvelope> parse(ConsumerRecord<String, String> record) {
        try {
            EventEnvelope event = mapper.readValue(record.value(), EventEnvelope.class);
            if (event.eventId() == null || event.type() == null || event.payload() == null) {
                throw new IllegalArgumentException("missing eventId, type or payload");
            }
            return Optional.of(event);
        } catch (JacksonException | IllegalArgumentException e) {
            skip(record, e.getMessage());
            return Optional.empty();
        }
    }

    /** Converts the payload to the given record type; on failure the message is logged and skipped. */
    public <T> Optional<T> payload(ConsumerRecord<String, String> record, EventEnvelope event, Class<T> type) {
        try {
            return Optional.of(mapper.treeToValue(event.payload(), type));
        } catch (JacksonException e) {
            skip(record, e.getMessage());
            return Optional.empty();
        }
    }

    /** True for messages produced after this service started (live), false for replayed ones. */
    public boolean isLive(ConsumerRecord<?, ?> record) {
        return Instant.ofEpochMilli(record.timestamp()).isAfter(serviceStartedAt);
    }

    public void skip(ConsumerRecord<?, ?> record, String reason) {
        log.warn("Skipping invalid message from {} [partition={}, offset={}]: {}",
                record.topic(), record.partition(), record.offset(), reason);
    }
}
