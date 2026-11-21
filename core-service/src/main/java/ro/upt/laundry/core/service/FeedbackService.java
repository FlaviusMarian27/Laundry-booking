package ro.upt.laundry.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.laundry.core.dto.CreateFeedbackRequest;
import ro.upt.laundry.core.dto.FeedbackResponse;
import ro.upt.laundry.core.exception.ApiException;
import ro.upt.laundry.core.model.Feedback;
import ro.upt.laundry.core.repository.FeedbackRepository;
import ro.upt.laundry.core.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedback;
    private final UserRepository users;

    public FeedbackService(FeedbackRepository feedback, UserRepository users) {
        this.feedback = feedback;
        this.users = users;
    }

    @Transactional
    public FeedbackResponse submit(CreateFeedbackRequest req) {
        if (!users.existsById(req.userId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found: " + req.userId());
        }
        Feedback saved = feedback.save(new Feedback(req.userId(), req.message().strip(), Instant.now()));
        return FeedbackResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listAll() {
        return feedback.findAllByOrderBySubmittedAtDesc().stream().map(FeedbackResponse::from).toList();
    }
}