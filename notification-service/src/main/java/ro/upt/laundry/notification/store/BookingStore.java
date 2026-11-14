package ro.upt.laundry.notification.store;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookingStore {

    private final Map<Long, BookingView> bookings = new ConcurrentHashMap<>();
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /** True if this event was already applied to the local copy. */
    public boolean isProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    /** Call only after the event was applied successfully. */
    public void markProcessed(String eventId) {
        processedEventIds.add(eventId);
    }

    public void save(BookingView booking) {
        bookings.put(booking.bookingId(), booking);
    }

    public boolean updateStatus(Long bookingId, String status) {
        return bookings.computeIfPresent(bookingId, (id, b) -> b.withStatus(status)) != null;
    }

    public Collection<BookingView> all() {
        return List.copyOf(bookings.values());
    }
}
