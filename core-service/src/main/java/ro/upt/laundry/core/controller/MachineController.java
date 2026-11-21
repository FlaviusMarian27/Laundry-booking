package ro.upt.laundry.core.controller;

import org.springframework.web.bind.annotation.*;
import ro.upt.laundry.core.dto.BookingResponse;
import ro.upt.laundry.core.dto.MachineResponse;
import ro.upt.laundry.core.service.BookingService;
import ro.upt.laundry.core.service.MachineService;

import java.util.List;

@RestController
@RequestMapping("/machines")
public class MachineController {

    private final MachineService machineService;
    private final BookingService bookingService;

    public MachineController(MachineService machineService, BookingService bookingService) {
        this.machineService = machineService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<MachineResponse> list() {
        return machineService.findAll();
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> activeBookings(@PathVariable Long id) {
        return bookingService.listActiveByMachine(id);
    }
    @PostMapping("/{id}/maintenance/close")
    public MachineResponse closeMaintenance(@PathVariable Long id) {
        return machineService.closeMaintenance(id);
    }
}