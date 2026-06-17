package com.easyfamily.feedback.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.feedback.dto.FeedbackDtos.FeedbackItem;
import com.easyfamily.feedback.dto.FeedbackDtos.FeedbackListResponse;
import com.easyfamily.feedback.dto.FeedbackDtos.ReplyRequest;
import com.easyfamily.feedback.entity.Feedback;
import com.easyfamily.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/feedback")
public class AdminFeedbackController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Lists feedback records with optional status filter.
     *
     * <p>GET /api/v1/admin/feedback?status=pending&amp;page=0&amp;size=20
     */
    @GetMapping
    public ApiResponse<FeedbackListResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Feedback> result = feedbackService.list(status, page, size);
        List<FeedbackItem> items = result.getContent().stream()
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(new FeedbackListResponse(items, result.getTotalElements(), page, size));
    }

    /**
     * Replies to a feedback entry and notifies the user via email.
     *
     * <p>POST /api/v1/admin/feedback/{id}/reply
     */
    @PostMapping("/{id}/reply")
    public ApiResponse<Void> reply(
            @PathVariable Long id,
            @Valid @RequestBody ReplyRequest req
    ) {
        try {
            feedbackService.reply(id, req.reply());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("FEEDBACK_NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            throw new BusinessException("FEEDBACK_ALREADY_REPLIED", e.getMessage());
        }
        return ApiResponse.ok(null);
    }

    private FeedbackItem toItem(Feedback f) {
        return new FeedbackItem(
                f.getId(),
                f.getUserId(),
                maskPhone(f.getPhone()),
                FeedbackItem.maskEmail(f.getEmail()),
                f.getTitle(),
                f.getDescription(),
                f.getStatus(),
                f.getReply(),
                f.getRepliedAt() != null ? f.getRepliedAt().format(DT_FMT) : null,
                f.getCreatedAt() != null ? f.getCreatedAt().format(DT_FMT) : null
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
