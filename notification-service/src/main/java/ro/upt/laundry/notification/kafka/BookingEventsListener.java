package ro.upt.laundry.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import ro.upt.laundry.notification.store.BookingStore;
import ro.upt.laundry.notification.store.BookingView;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

/**
 * Consumes bookings.events and keeps an in-memory copy of bookings.
 *
 * Idempotency: Kafka delivers at-least-once, so the same event can arrive twice.
 * We skip events whose eventId was already applied, and we mark an event as applied
 * only AFTER it was applied successfully. If applying throws, Spring Kafka retries
 * the record and the retry is processed normally instead of being treated as a duplicate.
 *
 * Concurrency: the listener runs on a single consumer thread, so the
 * "check isProcessed -> apply -> markProcessed" sequence is not interleaved.
 * HTTP threads only read the store (ConcurrentHashMap + immutable records).
 */
@Component
public class BookingEventsListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(BookingEventsListener.class);

    private final ObjectMapper mapper;
    private final BookingStore store;
    private final Instant serviceStartedAt = Instant.now();

    public BookingEventsListener(ObjectMapper mapper, BookingStore store) {
        this.mapper = mapper;
        this.store = store;
    }

    /** On every start, re-read the topic from the beginning to rebuild the in-memory copy. */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
        log.info("Rebuilding booking copy from the beginning of {}", assignments.keySet());
    }

    @KafkaListener(topics = "bookings.events")
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope event;
        BookingPayload p;
        try {
            event = mapper.readValue(record.value(), EventEnvelope.class);
            if (event.eventId() == null || event.type() == null || event.payload() == null) {
                throw new IllegalArgumentException("missing eventId, type or payload");
            }
            p = mapper.treeToValue(event.payload(), BookingPayload.class);
            if (p.bookingId() == null) {
                throw new IllegalArgumentException("missing bookingId");
            }
        } catch (JacksonException | IllegalArgumentException e) {
            log.warn("Skipping invalid message [partition={}, offset={}]: {}",
                    record.partition(), record.offset(), e.getMessage());
            return;
        }

        // 1. Check: already applied?
        if (store.isProcessed(event.eventId())) {
            log.info("Duplicate event {} ignored", event.eventId());
            return;
        }

        // Only notify for messages produced after this service started, not for replayed ones
        boolean live = Instant.ofEpochMilli(record.timestamp()).isAfter(serviceStartedAt);

        // 2. Apply (if this throws, the event is NOT marked and the retry will apply it)
        switch (event.type()) {
            case "booking.created" -> {
                store.save(new BookingView(p.bookingId(), p.userId(), p.machineId(),
                        p.startTime(), p.endTime(), "CREATED"));
                log.info("Booking {} stored (live={})", p.bookingId(), live);
                if (live) notify(p.userId(), "Rezervarea #%d pe mașina %d începe la %s"
                        .formatted(p.bookingId(), p.machineId(), p.startTime()));
            }
            case "booking.cancelled" -> {
                updateStatus(p.bookingId(), "CANCELLED");
                if (live) notify(p.userId(), "Rezervarea #%d a fost anulată".formatted(p.bookingId()));
            }
            case "booking.started" -> updateStatus(p.bookingId(), "STARTED");
            case "booking.noshow" -> {
                updateStatus(p.bookingId(), "NO_SHOW");
                if (live) notify(p.userId(), "Nu te-ai prezentat la rezervarea #%d".formatted(p.bookingId()));
            }
            default -> log.debug("Ignoring unknown event type {}", event.type());
        }

        // 3. Mark as applied only after success
        store.markProcessed(event.eventId());
    }

    private void updateStatus(Long bookingId, String status) {
        if (!store.updateStatus(bookingId, status)) {
            log.warn("Booking {} not found in local copy, cannot set status {}", bookingId, status);
        }
    }

    private void notify(Long userId, String message) {
        log.info("NOTIFICATION -> user {}: {}", userId, message);
    }
}
