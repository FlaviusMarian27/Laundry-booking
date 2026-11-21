package ro.upt.laundry.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/** Fields of the proposals.events payloads (proposal.created / approved / rejected). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposalPayload(Long proposalId, String kind, Long targetId, String reason,
                              String severity, String status,
                              Long decidedBy, Instant decidedAt) {
}
