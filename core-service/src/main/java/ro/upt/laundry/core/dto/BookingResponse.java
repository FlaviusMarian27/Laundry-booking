package ro.upt.laundry.core.dto;

import ro.upt.laundry.core.model.Booking;
import ro.upt.laundry.core.model.BookingStatus;
import java.time.Instant;

public record BookingResponse(
        Long id, Long userId, Long machineId,
        Instant startTime, Instant endTime,
        BookingStatus status, Instant startedAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getUserId(), b.getMachineId(),
                b.getStartTime(), b.getEndTime(), b.getStatus(), b.getStartedAt());
    }
}