package ro.upt.laundry.notification.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.upt.laundry.notification.store.BookingStore;
import ro.upt.laundry.notification.store.BookingView;

import java.util.Collection;

@RestController
public class BookingCopyController {

    private final BookingStore store;

    public BookingCopyController(BookingStore store) {
        this.store = store;
    }

    @GetMapping("/bookings")
    public Collection<BookingView> bookings() {
        return store.all();
    }
}
