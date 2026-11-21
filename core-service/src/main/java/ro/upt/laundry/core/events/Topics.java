package ro.upt.laundry.core.events;

/** Kafka topic names, as defined in docs/events.md. */
public final class Topics {

    public static final String BOOKINGS = "bookings.events";
    public static final String DEVICES = "devices.events";
    public static final String FEEDBACK = "feedback.events";
    public static final String PROPOSALS = "proposals.events";

    private Topics() {
    }
}
