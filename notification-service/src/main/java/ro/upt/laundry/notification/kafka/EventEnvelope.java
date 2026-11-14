package ro.upt.laundry.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(String eventId, String type, int version, Instant occurredAt, JsonNode payload) {
}
