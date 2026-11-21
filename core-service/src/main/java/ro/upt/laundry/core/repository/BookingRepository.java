package ro.upt.laundry.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.upt.laundry.core.model.Booking;
import ro.upt.laundry.core.model.BookingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Collection;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);
    List<Booking> findByMachineIdAndStatusInAndEndTimeAfterOrderByStartTime(
        Long machineId, Collection<BookingStatus> statuses, Instant after);
    List<Booking> findByMachineIdAndStatusAndEndTimeAfter(Long machineId, BookingStatus status, Instant after);
    
    List<Booking> findByStatusAndStartTimeBefore(BookingStatus status, Instant threshold);
}