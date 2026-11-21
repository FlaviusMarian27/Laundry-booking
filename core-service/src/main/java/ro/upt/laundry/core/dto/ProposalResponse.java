package ro.upt.laundry.core.dto;

import ro.upt.laundry.core.model.*;
import java.time.Instant;

public record ProposalResponse(
        Long id, ProposalKind kind, Long targetId, String reason,
        ProposalSeverity severity, ProposalStatus status,
        Instant createdAt, Long decidedBy, Instant decidedAt) {

    public static ProposalResponse from(Proposal p) {
        return new ProposalResponse(p.getId(), p.getKind(), p.getTargetId(), p.getReason(),
                p.getSeverity(), p.getStatus(), p.getCreatedAt(), p.getDecidedBy(), p.getDecidedAt());
    }
}