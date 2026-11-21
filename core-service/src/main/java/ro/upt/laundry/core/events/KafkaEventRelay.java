package ro.upt.laundry.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Sends recorded events to Kafka only AFTER the database transaction has committed.
 * If the transaction rolls back, the events are discarded, so Kafka never announces
 * something that was not saved.
 *
 * Events recorded in the same transaction are sent in the order they were recorded,
 * on the same thread. Together with the message key this keeps per-entity ordering.
 *
 * Limitation: if the application crashes between the commit and the send, the event is lost.
 * The robust alternative is a transactional outbox table (see core-service/README.md, open decisions).
 */
@Component
public class KafkaEventRelay {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventRelay.class);
    private static final int ENVELOPE_VERSION = 1;

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public KafkaEventRelay(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void relay(OutgoingEvent event) {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(), event.type(), ENVELOPE_VERSION, event.occurredAt(), event.payload());

        String json;
        try {
            json = mapper.writeValueAsString(envelope);
        } catch (JacksonException e) {
            log.error("Could not serialize event {} (key={}): {}", event.type(), event.key(), e.getMessage());
            return;
        }

        kafka.send(event.topic(), event.key(), json).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} (key={}) to {}: {}",
                        event.type(), event.key(), event.topic(), ex.getMessage());
            } else {
                log.info("Published {} eventId={} key={} -> {} [partition={}, offset={}]",
                        event.type(), envelope.eventId(), event.key(), event.topic(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
