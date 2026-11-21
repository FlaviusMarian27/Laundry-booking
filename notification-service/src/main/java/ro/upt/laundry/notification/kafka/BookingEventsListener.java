package ro.upt.laundry.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import ro.upt.laundry.notification.notify.Notifier;
import ro.upt.laundry.notification.store.BookingStore;
import ro.upt.laundry.notification.store.BookingView;

import java.util.Map;

/**
 * Consumes bookings.events and keeps an in-memory copy of bookings.
 *
 * Idempotency: Kafka delivers at-least-once, so the same event can arrive twice.
 * We skip events whose eventId was already applied, and we mark an event as applied
 * only AFTER it was applied successfully. If applying throws, Spring Kafka retries
 * the record and the retry is processed normally instead of being treated as a duplicate.
 *
 * Concurrency: each listener in this service runs on its own consumer thread and writes
 * only to its own store (bookings here). HTTP threads only read the stores
 * (ConcurrentHashMap + immutable records), so there is never more than one writer per store.
 */
@Component
public class BookingEventsListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(BookingEventsListener.class);

    private final EventSupport events;
    private final BookingStore store;
    private final Notifier notifier;

    public BookingEventsListener(EventSupport events, BookingStore store, Notifier notifier) {
        this.events = events;
        this.store = store;
        this.notifier = notifier;
    }

    /** On every start, re-read the topic from the beginning to rebuild the in-memory copy. */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
        log.info("Rebuilding booking copy from the beginning of {}", assignments.keySet());
    }

    @KafkaListener(topics = "bookings.events", groupId = "notification-service")
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope event = events.parse(record).orElse(null);
        if (event == null) {
            return;
        }
        BookingPayload p = events.payload(record, event, BookingPayload.class).orElse(null);
        if (p == null) {
            return;
        }
        if (p.bookingId() == null) {
            events.skip(record, "missing bookingId");
            return;
        }

        // 1. Check: already applied?
        if (store.isProcessed(event.eventId())) {
            log.info("Duplicate event {} ignored", event.eventId());
            return;
        }

        boolean live = events.isLive(record);

        // 2. Apply (if this throws, the event is NOT marked and the retry will apply it)
        switch (event.type()) {
            case "booking.created" -> {
                store.save(new BookingView(p.bookingId(), p.userId(), p.machineId(),
                        p.startTime(), p.endTime(), "CREATED"));
                log.info("Booking {} stored (live={})", p.bookingId(), live);
                if (live) notifier.toUser(p.userId(), "Rezervarea #%d pe mașina %d începe la %s"
                        .formatted(p.bookingId(), p.machineId(), p.startTime()));
            }
            case "booking.cancelled" -> {
                updateStatus(p.bookingId(), "CANCELLED");
                if (live) {
                    String message = "maintenance".equals(p.reason())
                            ? "Rezervarea #%d a fost anulată: mașina %d intră în mentenanță"
                                    .formatted(p.bookingId(), p.machineId())
                            : "Rezervarea #%d a fost anulată".formatted(p.bookingId());
                    notifier.toUser(p.userId(), message);
                }
            }
            case "booking.started" -> updateStatus(p.bookingId(), "STARTED");
            case "booking.noshow" -> {
                updateStatus(p.bookingId(), "NO_SHOW");
                if (live) notifier.toUser(p.userId(),
                        "Nu te-ai prezentat la rezervarea #%d".formatted(p.bookingId()));
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
}
