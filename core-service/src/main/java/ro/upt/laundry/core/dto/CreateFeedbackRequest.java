package ro.upt.laundry.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 2000) String message) {
}