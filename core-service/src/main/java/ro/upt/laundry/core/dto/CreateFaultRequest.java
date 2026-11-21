package ro.upt.laundry.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFaultRequest(
        @NotNull Long machineId,
        @NotNull Long reportedBy,
        @NotBlank @Size(max = 500) String description) {
}