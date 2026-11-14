package ro.upt.laundry.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingPayload(Long bookingId, Long userId, Long machineId,
                             Instant startTime, Instant endTime,
                             Instant startedAt, String reason) {
}
