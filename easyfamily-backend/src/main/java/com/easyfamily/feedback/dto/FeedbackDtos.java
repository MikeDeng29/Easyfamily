package com.easyfamily.feedback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class FeedbackDtos {

    public record FeedbackRequest(
            @NotBlank
            @Size(max = 2000)
            String description,

            @Size(max = 200)
            String title,

            @Email
            @Size(max = 200)
            String email
    ) {}

    public record FeedbackItem(
            Long id,
            String userId,
            String phone,
            String email,
            String title,
            String description,
            String status,
            String reply,
            String repliedAt,
            String createdAt
    ) {

        /**
         * Returns a masked representation of the email address suitable for display
         * in the admin panel.  Only the first character before {@code @} is kept;
         * the rest of the local-part is replaced with {@code ***}.
         * Example: {@code john.doe@gmail.com} → {@code j***@gmail.com}.
         * Returns {@code null} when the input is null or blank.
         */
        public static String maskEmail(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            int atIndex = raw.indexOf('@');
            if (atIndex <= 0) {
                return raw; // malformed — return as-is, no masking possible
            }
            return raw.charAt(0) + "***" + raw.substring(atIndex);
        }
    }

    public record ReplyRequest(
            @NotBlank
            @Size(max = 500)
            String reply
    ) {}

    public record FeedbackListResponse(
            List<FeedbackItem> items,
            long total,
            int page,
            int size
    ) {}
}
