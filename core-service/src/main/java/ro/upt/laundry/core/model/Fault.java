package ro.upt.laundry.core.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "faults")
public class Fault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long machineId;
    private Long reportedBy;
    private String description;
    private Instant reportedAt;

    protected Fault() {}

    public Fault(Long machineId, Long reportedBy, String description, Instant reportedAt) {
        this.machineId = machineId;
        this.reportedBy = reportedBy;
        this.description = description;
        this.reportedAt = reportedAt;
    }

    public Long getId() { return id; }
    public Long getMachineId() { return machineId; }
    public Long getReportedBy() { return reportedBy; }
    public String getDescription() { return description; }
    public Instant getReportedAt() { return reportedAt; }
}