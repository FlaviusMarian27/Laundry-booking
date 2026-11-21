package ro.upt.laundry.core.dto;

import ro.upt.laundry.core.model.Feedback;
import java.time.Instant;

public record FeedbackResponse(Long id, Long userId, String message, Instant submittedAt) {

    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(f.getId(), f.getUserId(), f.getMessage(), f.getSubmittedAt());
    }
}