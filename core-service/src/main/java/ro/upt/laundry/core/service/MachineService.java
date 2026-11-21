package ro.upt.laundry.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.laundry.core.dto.MachineResponse;
import ro.upt.laundry.core.exception.ApiException;
import ro.upt.laundry.core.events.EventPayloads;
import ro.upt.laundry.core.events.EventRecorder;
import ro.upt.laundry.core.events.Topics;
import ro.upt.laundry.core.model.Machine;
import ro.upt.laundry.core.model.MachineStatus;
import ro.upt.laundry.core.repository.MachineRepository;

import java.time.Clock;
import java.util.List;

@Service
public class MachineService {

    private final MachineRepository machineRepository;
    private final EventRecorder events;
    private final Clock clock;

    public MachineService(MachineRepository machineRepository, EventRecorder events, Clock clock) {
        this.machineRepository = machineRepository;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return machineRepository.findAll().stream().map(MachineResponse::from).toList();
    }

    @Transactional
    public MachineResponse closeMaintenance(Long id) {
        Machine m = machineRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Machine not found: " + id));
        if (m.getStatus() != MachineStatus.MAINTENANCE) {
            throw new ApiException(HttpStatus.CONFLICT, "Machine is not under maintenance");
        }
        m.markAvailable();
        events.record(Topics.DEVICES, m.getId(), "maintenance.closed",
                new EventPayloads.MaintenanceClosed(m.getId(), clock.instant()));
        return MachineResponse.from(m);
    }
}