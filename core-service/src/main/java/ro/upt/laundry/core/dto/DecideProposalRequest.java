package ro.upt.laundry.core.dto;

import jakarta.validation.constraints.NotNull;

public record DecideProposalRequest(@NotNull Long decidedBy) {
}