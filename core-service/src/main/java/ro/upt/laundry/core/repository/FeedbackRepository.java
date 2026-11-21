package ro.upt.laundry.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.upt.laundry.core.model.Feedback;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderBySubmittedAtDesc();
}