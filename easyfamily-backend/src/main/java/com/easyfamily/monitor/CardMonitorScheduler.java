package com.easyfamily.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "easyfamily.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class CardMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(CardMonitorScheduler.class);

    private final CardMonitorService cardMonitorService;
    private final JdbcTemplate jdbcTemplate;

    public CardMonitorScheduler(CardMonitorService cardMonitorService, JdbcTemplate jdbcTemplate) {
        this.cardMonitorService = cardMonitorService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${easyfamily.monitor.scan-cron:0 0 2 * * ?}")
    public void runDailyScan() {
        log.info("Card monitor daily scan starting");
        List<String> userIds = jdbcTemplate.query(
                "SELECT DISTINCT user_id FROM family_members",
                (rs, rowNum) -> rs.getString("user_id")
        );
        log.info("Card monitor scanning {} users", userIds.size());
        for (String userId : userIds) {
            try {
                cardMonitorService.scanAllFamilyMembers(userId);
            } catch (Exception e) {
                log.error("Card monitor scan failed for userId={}: {}", userId, e.getMessage(), e);
            }
        }
        log.info("Card monitor daily scan complete, processed {} users", userIds.size());
    }
}
