package ro.upt.laundry.core.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import ro.upt.laundry.core.dto.BookingResponse;
import ro.upt.laundry.core.dto.CreateBookingRequest;
import ro.upt.laundry.core.exception.ApiException;
import ro.upt.laundry.core.model.*;
import ro.upt.laundry.core.repository.BookingRepository;
import ro.upt.laundry.core.repository.MachineRepository;
import ro.upt.laundry.core.repository.UserRepository;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final MachineRepository machines;
    private final UserRepository users;
    private final Clock clock;
    private final Duration noShowGrace;

    public BookingService(BookingRepository bookings, MachineRepository machines, UserRepository users,
                      Clock clock,
                      @Value("${laundry.booking.noshow-grace:10m}") Duration noShowGrace) {
        this.bookings = bookings;
        this.machines = machines;
        this.users = users;
        this.clock = clock;
        this.noShowGrace = noShowGrace;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        if (req.startTime().isBefore(clock.instant())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startTime must be in the future");
        }
        if (!users.existsById(req.userId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found: " + req.userId());
        }
        Machine machine = machines.findById(req.machineId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + req.machineId()));
        if (machine.getStatus() != MachineStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "Machine is under maintenance");
        }
        try {
            Booking saved = bookings.saveAndFlush(
                    new Booking(req.userId(), req.machineId(), req.startTime(), req.endTime()));
            return BookingResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Machine is already booked in that interval");
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id) {
        return BookingResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listByUser(Long userId) {
        return bookings.findByUserIdOrderByStartTimeDesc(userId).stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse cancel(Long id) {
        Booking b = find(id);
        if (b.getStatus() != BookingStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a CREATED booking can be cancelled");
        }
        b.markCancelled();
        return BookingResponse.from(b);
    }

    @Transactional
    public BookingResponse start(Long id) {
        Booking b = find(id);
        Instant now = clock.instant();
        if (b.getStatus() != BookingStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a CREATED booking can be started");
        }
        if (now.isBefore(b.getStartTime()) || !now.isBefore(b.getEndTime())) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking can be started only during its time slot");
        }
        b.markStarted(now);
        return BookingResponse.from(b);
    }

    private Booking find(Long id) {
        return bookings.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listActiveByMachine(Long machineId) {
        if (!machines.existsById(machineId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + machineId);
        }
        return bookings.findByMachineIdAndStatusInAndEndTimeAfterOrderByStartTime(
                        machineId, List.of(BookingStatus.CREATED, BookingStatus.STARTED), clock.instant())
                .stream().map(BookingResponse::from).toList();
    }
    @Transactional
    public List<BookingResponse> cancelUpcomingForMachine(Long machineId) {
        List<Booking> upcoming = bookings.findByMachineIdAndStatusAndEndTimeAfter(
                machineId, BookingStatus.CREATED, clock.instant());
        upcoming.forEach(Booking::markCancelled);
        return upcoming.stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public List<BookingResponse> markNoShows() {
        Instant threshold = clock.instant().minus(noShowGrace);
        List<Booking> late = bookings.findByStatusAndStartTimeBefore(BookingStatus.CREATED, threshold);
        late.forEach(Booking::markNoShow);
        return late.stream().map(BookingResponse::from).toList();
    }
}