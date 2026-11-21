package ro.upt.laundry.notification.store;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copy of the AI agent's proposals built from proposals.events.
 * Written only by ProposalEventsListener's consumer thread; read by HTTP threads.
 */
@Component
public class ProposalStore {

    private final Map<Long, ProposalView> proposals = new ConcurrentHashMap<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public boolean isProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    public void markProcessed(String eventId) {
        processedEventIds.add(eventId);
    }

    public void save(ProposalView proposal) {
        proposals.put(proposal.proposalId(), proposal);
    }

    public boolean decide(Long proposalId, String status, Long decidedBy, Instant decidedAt) {
        return proposals.computeIfPresent(proposalId, (id, p) -> p.decided(status, decidedBy, decidedAt)) != null;
    }

    public Collection<ProposalView> all() {
        return List.copyOf(proposals.values());
    }
}
