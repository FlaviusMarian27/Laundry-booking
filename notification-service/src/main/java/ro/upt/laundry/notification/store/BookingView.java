package ro.upt.laundry.notification.store;

import java.time.Instant;

public record BookingView(Long bookingId, Long userId, Long machineId,
                          Instant startTime, Instant endTime, String status) {

    public BookingView withStatus(String newStatus) {
        return new BookingView(bookingId, userId, machineId, startTime, endTime, newStatus);
    }
}
