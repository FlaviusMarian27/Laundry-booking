package ro.upt.laundry.notification.store;

import java.time.Instant;

public record MachineView(Long machineId, String status, int faultCount, Instant lastFaultAt) {

    public static MachineView unknown(Long machineId) {
        return new MachineView(machineId, "AVAILABLE", 0, null);
    }

    public MachineView withStatus(String newStatus) {
        return new MachineView(machineId, newStatus, faultCount, lastFaultAt);
    }

    public MachineView withFault(Instant at) {
        return new MachineView(machineId, status, faultCount + 1, at);
    }
}
