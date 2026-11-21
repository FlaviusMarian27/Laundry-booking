package ro.upt.laundry.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private MachineType type;

    private String building;

    @Enumerated(EnumType.STRING)
    private MachineStatus status;

    protected Machine() {}   // cerut de JPA

    public Long getId() { return id; }
    public String getName() { return name; }
    public MachineType getType() { return type; }
    public String getBuilding() { return building; }
    public MachineStatus getStatus() { return status; }
    public void markMaintenance() { this.status = MachineStatus.MAINTENANCE; }
    public void markAvailable()   { this.status = MachineStatus.AVAILABLE; }
}