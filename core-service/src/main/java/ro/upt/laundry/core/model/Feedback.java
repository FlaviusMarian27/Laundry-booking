package ro.upt.laundry.core.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String message;
    private Instant submittedAt;

    protected Feedback() {}

    public Feedback(Long userId, String message, Instant submittedAt) {
        this.userId = userId;
        this.message = message;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getMessage() { return message; }
    public Instant getSubmittedAt() { return submittedAt; }
}