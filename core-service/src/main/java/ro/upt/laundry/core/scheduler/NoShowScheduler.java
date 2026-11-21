package ro.upt.laundry.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.upt.laundry.core.dto.BookingResponse;
import ro.upt.laundry.core.service.BookingService;

import java.util.List;

@Component
public class NoShowScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoShowScheduler.class);

    private final BookingService bookingService;

    public NoShowScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${laundry.booking.noshow-check-ms:60000}")
    public void markNoShows() {
        List<BookingResponse> marked = bookingService.markNoShows();
        if (!marked.isEmpty()) {
            log.info("Marked {} booking(s) as NO_SHOW: {}", marked.size(),
                    marked.stream().map(BookingResponse::id).toList());
        }
    }
}