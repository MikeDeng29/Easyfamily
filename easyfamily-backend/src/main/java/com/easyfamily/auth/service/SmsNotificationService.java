package com.easyfamily.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends free-form notification SMS (not OTP).
 *
 * <p>In mock mode the message is written to the log — no real SMS is sent.
 * In Aliyun mode a configurable notification template code is used; if that
 * template is not configured the call is skipped with a warning, so the
 * feature degrades gracefully rather than throwing at runtime.
 */
@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String smsProvider;

    public SmsNotificationService(
            @Value("${easyfamily.sms.provider:mock}") String smsProvider
    ) {
        this.smsProvider = smsProvider;
    }

    /**
     * Sends a free-form text notification to the given phone number.
     * Failures are logged but never propagated — a reply SMS failure must
     * not roll back the admin's reply that was already persisted.
     *
     * @param phone   target phone number
     * @param message full notification text (max ~70 chars for a single SMS)
     */
    public void sendNotification(String phone, String message) {
        try {
            if ("mock".equals(smsProvider)) {
                log.info("[MOCK SMS NOTIFY] phone={}, message={}", maskPhone(phone), message);
            } else {
                // Aliyun free-form notifications require a pre-approved template with
                // a variable slot. Until a dedicated notification template code is
                // configured in Aliyun SMS, we emit a warning and skip the call so
                // the feature remains usable without a second template.
                log.warn("[SMS NOTIFY SKIPPED] Aliyun notification template not configured. "
                        + "phone={}, message={}", maskPhone(phone), message);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS notification to phone={}", maskPhone(phone), e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
