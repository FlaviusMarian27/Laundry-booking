package ro.upt.laundry.core.dto;

import ro.upt.laundry.core.model.Machine;
import ro.upt.laundry.core.model.MachineStatus;
import ro.upt.laundry.core.model.MachineType;

public record MachineResponse(
        Long id, String name, MachineType type, String building, MachineStatus status) {

    public static MachineResponse from(Machine m) {
        return new MachineResponse(m.getId(), m.getName(), m.getType(), m.getBuilding(), m.getStatus());
    }
}