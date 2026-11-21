package ro.upt.laundry.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/** Fields of the devices.events payloads (fault.reported, maintenance.opened, maintenance.closed). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DevicePayload(Long machineId,
                            Long faultId, Long reportedBy, String description, Instant reportedAt,
                            Long proposalId, Instant openedAt, Instant closedAt) {
}
