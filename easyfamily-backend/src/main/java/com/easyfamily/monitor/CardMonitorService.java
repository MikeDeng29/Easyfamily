package com.easyfamily.monitor;

import com.easyfamily.ai.llm.LlmProvider;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberItem;
import com.easyfamily.query.service.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CardMonitorService {

    private static final Logger log = LoggerFactory.getLogger(CardMonitorService.class);

    private static final String SYSTEM_PROMPT =
            "你是家庭财务安全助手。用简短中文（不超过100字）分析以下手机号银行卡绑定信息，" +
            "指出潜在风险（如绑定卡数量异常、疑似休眠账户等），" +
            "给出LOW/MEDIUM/HIGH风险级别，格式：【风险级别】\n分析内容";

    private final JdbcTemplate jdbcTemplate;
    private final QueryService queryService;
    private final LlmProvider llmProvider;

    public CardMonitorService(JdbcTemplate jdbcTemplate, QueryService queryService, LlmProvider llmProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryService = queryService;
        this.llmProvider = llmProvider;
    }

    public void scanAllFamilyMembers(String userId) {
        List<FamilyMemberItem> members = jdbcTemplate.query(
                """
                        SELECT member_id, member_name, member_phone, relation_to_user
                        FROM family_members
                        WHERE user_id = ? AND member_phone IS NOT NULL
                        ORDER BY member_id
                        """,
                (rs, rowNum) -> new FamilyMemberItem(
                        rs.getString("member_id"),
                        rs.getString("member_name"),
                        rs.getString("member_phone"),
                        rs.getString("relation_to_user")
                ),
                userId
        );

        for (FamilyMemberItem member : members) {
            try {
                scanMember(userId, member);
            } catch (Exception e) {
                log.error("Monitor scan failed for userId={} memberId={} name={}: {}", userId, member.memberId(), member.name(), e.getMessage(), e);
            }
        }
    }

    private void scanMember(String userId, FamilyMemberItem member) {
        String newSnapshot = queryService.queryForMonitor(member.phone());

        String previousSnapshot = jdbcTemplate.query(
                """
                        SELECT snapshot_json
                        FROM card_monitor_snapshot
                        WHERE member_id = ?
                        ORDER BY checked_at DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString(1) : null,
                member.memberId()
        );

        boolean changed = !Objects.equals(newSnapshot, previousSnapshot);

        String riskLevel = null;
        String riskSummary = null;

        if (changed || previousSnapshot == null) {
            RiskAnalysis analysis = analyzeRisk(member.name(), member.relation(), newSnapshot);
            riskLevel = analysis.riskLevel();
            riskSummary = analysis.summary();
        } else {
            // Re-use risk from the latest snapshot when data has not changed
            RiskFromDb existing = jdbcTemplate.query(
                    """
                            SELECT risk_level, risk_summary
                            FROM card_monitor_snapshot
                            WHERE member_id = ?
                            ORDER BY checked_at DESC
                            LIMIT 1
                            """,
                    rs -> rs.next()
                            ? new RiskFromDb(rs.getString("risk_level"), rs.getString("risk_summary"))
                            : null,
                    member.memberId()
            );
            if (existing != null) {
                riskLevel = existing.riskLevel();
                riskSummary = existing.riskSummary();
            }
        }

        jdbcTemplate.update(
                """
                        INSERT INTO card_monitor_snapshot
                            (user_id, member_id, member_phone, snapshot_json, card_count, risk_level, risk_summary, checked_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))
                        """,
                userId,
                member.memberId(),
                member.phone(),
                newSnapshot,
                0,
                riskLevel,
                riskSummary
        );
        log.info("Monitor snapshot saved: userId={} memberId={} changed={} riskLevel={}", userId, member.memberId(), changed, riskLevel);
    }

    private RiskAnalysis analyzeRisk(String memberName, String relation, String snapshotJson) {
        String userMessage = "家庭成员：" + memberName + "（" + relation + "）\n绑定数据：" + snapshotJson;
        String response;
        try {
            response = llmProvider.chat(SYSTEM_PROMPT, userMessage);
        } catch (Exception e) {
            log.warn("LLM analysis failed: {}", e.getMessage());
            return new RiskAnalysis("LOW", "AI分析暂时不可用，请稍后查看详情。");
        }

        String riskLevel = extractRiskLevel(response);
        return new RiskAnalysis(riskLevel, response.trim());
    }

    private String extractRiskLevel(String response) {
        if (response == null) return "LOW";
        if (response.contains("【HIGH】")) return "HIGH";
        if (response.contains("【MEDIUM】")) return "MEDIUM";
        if (response.contains("【LOW】")) return "LOW";
        return "LOW";
    }

    public List<MonitorSnapshotItem> getLatestSnapshots(String userId) {
        return jdbcTemplate.query(
                """
                        SELECT s.member_id,
                               f.member_name,
                               s.member_phone,
                               s.risk_level,
                               s.risk_summary,
                               s.card_count,
                               s.checked_at
                        FROM card_monitor_snapshot s
                        JOIN family_members f ON f.member_id = s.member_id AND f.user_id = s.user_id
                        WHERE s.id IN (
                            SELECT MAX(id)
                            FROM card_monitor_snapshot
                            WHERE user_id = ?
                            GROUP BY member_id
                        )
                        ORDER BY s.member_id
                        """,
                (rs, rowNum) -> new MonitorSnapshotItem(
                        rs.getString("member_id"),
                        rs.getString("member_name"),
                        maskPhone(rs.getString("member_phone")),
                        rs.getString("risk_level"),
                        rs.getString("risk_summary"),
                        rs.getInt("card_count"),
                        rs.getObject("checked_at", LocalDateTime.class)
                ),
                userId
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private record RiskAnalysis(String riskLevel, String summary) {}

    private record RiskFromDb(String riskLevel, String riskSummary) {}
}
