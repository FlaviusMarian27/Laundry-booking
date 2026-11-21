package ro.upt.laundry.core.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.upt.laundry.core.dto.BookingResponse;
import ro.upt.laundry.core.dto.CreateBookingRequest;
import ro.upt.laundry.core.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return bookingService.get(id);
    }

    @GetMapping
    public List<BookingResponse> listByUser(@RequestParam Long userId) {
        return bookingService.listByUser(userId);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @PostMapping("/{id}/start")
    public BookingResponse start(@PathVariable Long id) {
        return bookingService.start(id);
    }
}