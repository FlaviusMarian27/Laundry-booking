package ro.upt.laundry.core.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long machineId;
    private Instant startTime;
    private Instant endTime;
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    
    private Instant startedAt;
    
    protected Booking() {}

    @Version
    private Long version;
    
    public Booking(Long userId, Long machineId, Instant startTime, Instant endTime) {
        this.userId = userId;
        this.machineId = machineId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = BookingStatus.CREATED;
    }

    public void markCancelled() { this.status = BookingStatus.CANCELLED; }

    public void markStarted(Instant at) {
        this.status = BookingStatus.STARTED;
        this.startedAt = at;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getMachineId() { return machineId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }

    public void markNoShow() { this.status = BookingStatus.NO_SHOW; }
}