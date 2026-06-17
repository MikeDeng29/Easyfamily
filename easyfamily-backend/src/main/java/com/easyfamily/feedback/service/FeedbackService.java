package com.easyfamily.feedback.service;

import com.easyfamily.auth.service.SmsNotificationService;
import com.easyfamily.common.service.EmailNotificationService;
import com.easyfamily.feedback.entity.Feedback;
import com.easyfamily.feedback.repository.FeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class FeedbackService {

    private static final int TITLE_MAX_DISPLAY_LEN = 20;

    private final FeedbackRepository feedbackRepository;
    /** Kept for potential future use; SMS notification is no longer sent on reply. */
    private final SmsNotificationService smsNotificationService;
    private final EmailNotificationService emailNotificationService;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            SmsNotificationService smsNotificationService,
            EmailNotificationService emailNotificationService
    ) {
        this.feedbackRepository = feedbackRepository;
        this.smsNotificationService = smsNotificationService;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Persists a new feedback record with status "pending".
     *
     * @param email optional notification email address; may be {@code null}
     */
    public Feedback save(String userId, String phone, String title, String description, String email) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setPhone(phone);
        feedback.setEmail(email);
        feedback.setTitle(title);
        feedback.setDescription(description);
        feedback.setStatus("pending");
        return feedbackRepository.save(feedback);
    }

    /**
     * Returns a page of feedback records ordered by creation time descending.
     *
     * @param status filter by status; pass {@code null} to return all records
     */
    @Transactional(readOnly = true)
    public Page<Feedback> list(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status == null || status.isBlank()) {
            return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * Records an admin reply, sets status to "replied", and notifies the user
     * via SMS.  Only feedback in "pending" status may be replied to.
     *
     * @throws IllegalStateException if the feedback has already been replied to or closed
     */
    public void reply(Long id, String replyContent) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("feedback not found: " + id));

        if (!"pending".equals(feedback.getStatus())) {
            throw new IllegalStateException("already replied");
        }

        feedback.setReply(replyContent);
        feedback.setRepliedAt(LocalDateTime.now());
        feedback.setStatus("replied");
        feedbackRepository.save(feedback);

        emailNotificationService.sendFeedbackReply(
                feedback.getEmail(),
                feedback.getTitle(),
                replyContent
        );
    }

    /**
     * Returns the title to display in the SMS, falling back to a prefix of the
     * description when no title is set.  Truncates to {@value #TITLE_MAX_DISPLAY_LEN}
     * characters with an ellipsis.
     */
    private String buildDisplayTitle(String title, String description) {
        String base = (title != null && !title.isBlank()) ? title : description;
        if (base.length() > TITLE_MAX_DISPLAY_LEN) {
            return base.substring(0, TITLE_MAX_DISPLAY_LEN) + "...";
        }
        return base;
    }
}
