package com.easyfamily.monitor;

import java.time.LocalDateTime;

public record MonitorSnapshotItem(
        String memberId,
        String memberName,
        String memberPhone,
        String riskLevel,
        String riskSummary,
        int cardCount,
        LocalDateTime checkedAt
) {
}
