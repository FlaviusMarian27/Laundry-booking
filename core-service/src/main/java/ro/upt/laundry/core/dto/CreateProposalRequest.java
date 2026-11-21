package ro.upt.laundry.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.upt.laundry.core.model.ProposalKind;
import ro.upt.laundry.core.model.ProposalSeverity;

public record CreateProposalRequest(
        @NotNull ProposalKind kind,
        @NotNull Long targetId,
        @NotBlank @Size(max = 500) String reason,
        ProposalSeverity severity) {
}