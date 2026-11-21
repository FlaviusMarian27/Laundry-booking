package ro.upt.laundry.core.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Services call this from inside their @Transactional methods.
 * Nothing is sent to Kafka here: the event is only published inside the application and
 * KafkaEventRelay sends it after the transaction commits (or drops it on rollback).
 */
@Component
public class EventRecorder {

    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public EventRecorder(ApplicationEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * @param topic   one of {@link Topics}
     * @param key     message key (entity id), so all events of one entity go to the same partition and keep their order
     * @param type    event type, e.g. "booking.created"
     * @param payload one of the records from {@link EventPayloads}
     */
    public void record(String topic, Object key, String type, Object payload) {
        publisher.publishEvent(new OutgoingEvent(topic, String.valueOf(key), type, clock.instant(), payload));
    }
}
