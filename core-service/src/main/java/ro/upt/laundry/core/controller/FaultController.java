package ro.upt.laundry.core.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.upt.laundry.core.dto.CreateFaultRequest;
import ro.upt.laundry.core.dto.FaultResponse;
import ro.upt.laundry.core.service.FaultService;

import java.util.List;

@RestController
@RequestMapping("/faults")
public class FaultController {

    private final FaultService faultService;

    public FaultController(FaultService faultService) {
        this.faultService = faultService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaultResponse report(@Valid @RequestBody CreateFaultRequest request) {
        return faultService.report(request);
    }

    @GetMapping
    public List<FaultResponse> list(@RequestParam Long machineId) {
        return faultService.listByMachine(machineId);
    }
}