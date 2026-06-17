package com.easyfamily.common.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${easyfamily.mail.enabled:false}")
    private boolean enabled;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an admin-reply notification email to the feedback submitter.
     * Failures are logged but never propagated so that a mail delivery failure
     * cannot roll back a reply that has already been persisted.
     */
    public void sendFeedbackReply(String toEmail, String feedbackTitle, String replyContent) {
        if (!enabled || fromAddress.isBlank()) {
            log.info("[Email mock] to={} title={} reply={}", toEmail, feedbackTitle, replyContent);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("No email address for feedback reply notification, skipping");
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("青鸟管家：你的反馈已收到回复");
            String body = String.format("""
                你好，

                感谢你提交的反馈："%s"

                管理员回复：
                %s

                如有其他问题，欢迎继续在应用内反馈。

                — 青鸟管家团队
                """, feedbackTitle != null && !feedbackTitle.isBlank() ? feedbackTitle : "（无标题）", replyContent);
            helper.setText(body);
            mailSender.send(msg);
            log.info("Feedback reply email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send feedback reply email to {}", toEmail, e);
        }
    }
}
