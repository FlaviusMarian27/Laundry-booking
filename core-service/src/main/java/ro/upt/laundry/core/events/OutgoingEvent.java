package ro.upt.laundry.core.events;

import java.time.Instant;

/**
 * An event recorded inside a database transaction. It is only an in-memory Spring event;
 * KafkaEventRelay sends it to Kafka after the transaction commits.
 */
public record OutgoingEvent(String topic, String key, String type, Instant occurredAt, Object payload) {
}
