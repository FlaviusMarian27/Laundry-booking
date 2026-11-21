package ro.upt.laundry.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import ro.upt.laundry.notification.notify.Notifier;
import ro.upt.laundry.notification.store.ProposalStore;
import ro.upt.laundry.notification.store.ProposalView;

import java.util.Map;

/**
 * Consumes proposals.events: the AI agent's proposals and the admin's decisions.
 * Keeps a copy of proposals and notifies the admin when the agent proposes something.
 * Runs on its own consumer thread and consumer group, and writes only to ProposalStore.
 */
@Component
public class ProposalEventsListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(ProposalEventsListener.class);

    private final EventSupport events;
    private final ProposalStore store;
    private final Notifier notifier;

    public ProposalEventsListener(EventSupport events, ProposalStore store, Notifier notifier) {
        this.events = events;
        this.store = store;
        this.notifier = notifier;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
        log.info("Rebuilding proposal copy from the beginning of {}", assignments.keySet());
    }

    @KafkaListener(topics = "proposals.events", groupId = "notification-service-proposals")
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope event = events.parse(record).orElse(null);
        if (event == null) {
            return;
        }
        ProposalPayload p = events.payload(record, event, ProposalPayload.class).orElse(null);
        if (p == null) {
            return;
        }
        if (p.proposalId() == null) {
            events.skip(record, "missing proposalId");
            return;
        }
        if (store.isProcessed(event.eventId())) {
            log.info("Duplicate event {} ignored", event.eventId());
            return;
        }

        boolean live = events.isLive(record);

        switch (event.type()) {
            case "proposal.created" -> {
                store.save(new ProposalView(p.proposalId(), p.kind(), p.targetId(), p.reason(),
                        p.severity(), "PENDING", null, null));
                if (live) {
                    String target = "MAINTENANCE".equals(p.kind()) ? "mașina" : "utilizatorul";
                    notifier.toAdmin("Propunere nouă de la agent (#%d): %s pentru %s %d, severitate %s. Motiv: %s"
                            .formatted(p.proposalId(), p.kind(), target, p.targetId(), p.severity(), p.reason()));
                }
            }
            case "proposal.approved", "proposal.rejected" -> {
                String status = event.type().equals("proposal.approved") ? "APPROVED" : "REJECTED";
                if (!store.decide(p.proposalId(), status, p.decidedBy(), p.decidedAt())) {
                    log.warn("Proposal {} not found in local copy, cannot set status {}", p.proposalId(), status);
                }
                if (live) notifier.toAdmin("Propunerea #%d a fost %s de adminul %d".formatted(
                        p.proposalId(), status.equals("APPROVED") ? "aprobată" : "respinsă", p.decidedBy()));
            }
            default -> log.debug("Ignoring unknown event type {}", event.type());
        }

        store.markProcessed(event.eventId());
    }
}
