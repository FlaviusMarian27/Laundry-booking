package ro.upt.laundry.core.dto;

import ro.upt.laundry.core.model.Fault;
import java.time.Instant;

public record FaultResponse(Long id, Long machineId, Long reportedBy, String description, Instant reportedAt) {

    public static FaultResponse from(Fault f) {
        return new FaultResponse(f.getId(), f.getMachineId(), f.getReportedBy(),
                f.getDescription(), f.getReportedAt());
    }
}