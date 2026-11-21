package ro.upt.laundry.core.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateBookingRequest(
        @NotNull Long userId,
        @NotNull Long machineId,
        @NotNull Instant startTime,
        @NotNull Instant endTime) {
}