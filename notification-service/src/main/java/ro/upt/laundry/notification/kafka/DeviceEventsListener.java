package ro.upt.laundry.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import ro.upt.laundry.notification.notify.Notifier;
import ro.upt.laundry.notification.store.MachineStore;

import java.util.Map;

/**
 * Consumes devices.events: faults and maintenance.
 * Keeps a copy of machine state (status, fault count) and notifies the admin / students.
 * Runs on its own consumer thread and consumer group, and writes only to MachineStore.
 */
@Component
public class DeviceEventsListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventsListener.class);

    private final EventSupport events;
    private final MachineStore store;
    private final Notifier notifier;

    public DeviceEventsListener(EventSupport events, MachineStore store, Notifier notifier) {
        this.events = events;
        this.store = store;
        this.notifier = notifier;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
        log.info("Rebuilding machine copy from the beginning of {}", assignments.keySet());
    }

    @KafkaListener(topics = "devices.events", groupId = "notification-service-devices")
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope event = events.parse(record).orElse(null);
        if (event == null) {
            return;
        }
        DevicePayload p = events.payload(record, event, DevicePayload.class).orElse(null);
        if (p == null) {
            return;
        }
        if (p.machineId() == null) {
            events.skip(record, "missing machineId");
            return;
        }
        if (store.isProcessed(event.eventId())) {
            log.info("Duplicate event {} ignored", event.eventId());
            return;
        }

        boolean live = events.isLive(record);

        switch (event.type()) {
            case "fault.reported" -> {
                store.recordFault(p.machineId(), p.reportedAt());
                if (live) notifier.toAdmin("Defect raportat la mașina %d de utilizatorul %d: %s"
                        .formatted(p.machineId(), p.reportedBy(), p.description()));
            }
            case "maintenance.opened" -> {
                store.setStatus(p.machineId(), "MAINTENANCE");
                if (live) notifier.toAllStudents(
                        "Mașina %d a intrat în mentenanță".formatted(p.machineId()));
            }
            case "maintenance.closed" -> {
                store.setStatus(p.machineId(), "AVAILABLE");
                if (live) notifier.toAllStudents(
                        "Mașina %d este din nou disponibilă".formatted(p.machineId()));
            }
            default -> log.debug("Ignoring unknown event type {}", event.type());
        }

        store.markProcessed(event.eventId());
    }
}
