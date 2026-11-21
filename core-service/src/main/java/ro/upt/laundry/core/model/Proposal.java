package ro.upt.laundry.core.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProposalKind kind;

    private Long targetId;
    private String reason;

    @Enumerated(EnumType.STRING)
    private ProposalSeverity severity;

    @Enumerated(EnumType.STRING)
    private ProposalStatus status;

    private Instant createdAt;
    private Long decidedBy;
    private Instant decidedAt;

    protected Proposal() {}

    public Proposal(ProposalKind kind, Long targetId, String reason,
                    ProposalSeverity severity, Instant createdAt) {
        this.kind = kind;
        this.targetId = targetId;
        this.reason = reason;
        this.severity = severity;
        this.createdAt = createdAt;
        this.status = ProposalStatus.PENDING;
    }

    public void decide(ProposalStatus newStatus, Long adminId, Instant at) {
        this.status = newStatus;
        this.decidedBy = adminId;
        this.decidedAt = at;
    }

    public Long getId() { return id; }
    public ProposalKind getKind() { return kind; }
    public Long getTargetId() { return targetId; }
    public String getReason() { return reason; }
    public ProposalSeverity getSeverity() { return severity; }
    public ProposalStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}