package ro.upt.laundry.core.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.upt.laundry.core.dto.CreateFeedbackRequest;
import ro.upt.laundry.core.dto.FeedbackResponse;
import ro.upt.laundry.core.service.FeedbackService;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@Valid @RequestBody CreateFeedbackRequest request) {
        return feedbackService.submit(request);
    }

    @GetMapping
    public List<FeedbackResponse> list() {
        return feedbackService.listAll();
    }
}