package ro.upt.laundry.core.events;

import java.time.Instant;

/** The common envelope of every event (docs/events.md). */
public record EventEnvelope(String eventId, String type, int version, Instant occurredAt, Object payload) {
}
