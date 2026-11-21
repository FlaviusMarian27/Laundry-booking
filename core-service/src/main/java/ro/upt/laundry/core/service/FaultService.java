package ro.upt.laundry.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.laundry.core.dto.CreateFaultRequest;
import ro.upt.laundry.core.dto.FaultResponse;
import ro.upt.laundry.core.exception.ApiException;
import ro.upt.laundry.core.events.EventPayloads;
import ro.upt.laundry.core.events.EventRecorder;
import ro.upt.laundry.core.events.Topics;
import ro.upt.laundry.core.model.Fault;
import ro.upt.laundry.core.repository.FaultRepository;
import ro.upt.laundry.core.repository.MachineRepository;
import ro.upt.laundry.core.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
public class FaultService {

    private final FaultRepository faults;
    private final MachineRepository machines;
    private final UserRepository users;
    private final EventRecorder events;

    public FaultService(FaultRepository faults, MachineRepository machines, UserRepository users,
                        EventRecorder events) {
        this.faults = faults;
        this.machines = machines;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public FaultResponse report(CreateFaultRequest req) {
        if (!machines.existsById(req.machineId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + req.machineId());
        }
        if (!users.existsById(req.reportedBy())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found: " + req.reportedBy());
        }
        Fault saved = faults.save(
                new Fault(req.machineId(), req.reportedBy(), req.description().strip(), Instant.now()));
        events.record(Topics.DEVICES, saved.getMachineId(), "fault.reported",
                new EventPayloads.FaultReported(saved.getId(), saved.getMachineId(), saved.getReportedBy(),
                        saved.getDescription(), saved.getReportedAt()));
        return FaultResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FaultResponse> listByMachine(Long machineId) {
        if (!machines.existsById(machineId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + machineId);
        }
        return faults.findByMachineIdOrderByReportedAtDesc(machineId).stream().map(FaultResponse::from).toList();
    }
}