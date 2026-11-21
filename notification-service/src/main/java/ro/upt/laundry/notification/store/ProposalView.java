package ro.upt.laundry.notification.store;

import java.time.Instant;

public record ProposalView(Long proposalId, String kind, Long targetId, String reason, String severity,
                           String status, Long decidedBy, Instant decidedAt) {

    public ProposalView decided(String newStatus, Long by, Instant at) {
        return new ProposalView(proposalId, kind, targetId, reason, severity, newStatus, by, at);
    }
}
