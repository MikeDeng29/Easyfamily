package com.easyfamily.feedback.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.feedback.dto.FeedbackDtos.FeedbackRequest;
import com.easyfamily.feedback.service.FeedbackService;
import com.easyfamily.feedback.service.GithubIssueService;
import com.easyfamily.security.AuthContext;
import com.easyfamily.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final GithubIssueService githubIssueService;
    private final UserProfileService userProfileService;

    public FeedbackController(
            FeedbackService feedbackService,
            GithubIssueService githubIssueService,
            UserProfileService userProfileService) {
        this.feedbackService = feedbackService;
        this.githubIssueService = githubIssueService;
        this.userProfileService = userProfileService;
    }

    @PostMapping
    public ApiResponse<Void> submit(@Valid @RequestBody FeedbackRequest req) {
        var user = AuthContext.currentUser();

        String phone = user.phone();
        String masked = phone.length() >= 7
                ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4)
                : "***";

        String title = (req.title() != null && !req.title().isBlank())
                ? req.title()
                : "用户反馈 · " + masked;

        // Resolve the notification email: prefer the address supplied in the request;
        // fall back to the email stored in the user's profile.
        String email = req.email();
        if (email == null || email.isBlank()) {
            var profile = userProfileService.getProfile(user.userId(), phone);
            email = profile.email();
        }

        // Persist feedback record before calling the external GitHub API so that
        // the record is always saved even if the GitHub call fails.
        feedbackService.save(user.userId(), phone, title, req.description(), email);

        String body = String.format("""
                ## 用户反馈

                **用户ID**: %s
                **手机号**: %s
                **时间**: %s

                ## 问题描述

                %s
                """,
                user.userId(),
                masked,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                req.description()
        );

        githubIssueService.createIssue(title, body);
        return ApiResponse.ok(null);
    }
}
