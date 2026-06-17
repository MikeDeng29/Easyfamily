package com.easyfamily.bill.service;

import com.easyfamily.bill.dto.BillDtos.BillItem;
import com.easyfamily.bill.dto.BillDtos.BillStatsDto;
import com.easyfamily.bill.dto.BillDtos.CategoryStat;
import com.easyfamily.bill.dto.BillDtos.CreateBillRequest;
import com.easyfamily.bill.dto.BillDtos.FamilyBillStats;
import com.easyfamily.bill.dto.BillDtos.MemberStats;
import com.easyfamily.bill.dto.BillDtos.MonthlyTrendItem;
import com.easyfamily.bill.dto.BillDtos.SecurityReportDto;
import com.easyfamily.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BillService {

    private final JdbcTemplate jdbcTemplate;

    public BillService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BillItem create(String userId, CreateBillRequest req) {
        LocalDate billedAt = parseDate(req.billedAt());
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO bill(user_id, direction, category, amount, note, billed_at, created_at, updated_at)" +
                    " VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, req.direction());
            ps.setString(3, req.category());
            ps.setBigDecimal(4, req.amount());
            ps.setString(5, req.note());
            ps.setDate(6, Date.valueOf(billedAt));
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new BillItem(id, req.direction(), req.category(), req.amount(), req.note(),
                req.billedAt(), System.currentTimeMillis());
    }

    public List<BillItem> list(String userId, String month) {
        if (month != null && !month.isBlank()) {
            var range = monthRange(month);
            return jdbcTemplate.query(
                    "SELECT id, direction, category, amount, note, billed_at, created_at FROM bill" +
                    " WHERE user_id = ? AND billed_at BETWEEN ? AND ? ORDER BY billed_at DESC, id DESC",
                    (rs, rowNum) -> new BillItem(
                            rs.getLong("id"),
                            rs.getString("direction"),
                            rs.getString("category"),
                            rs.getBigDecimal("amount"),
                            rs.getString("note"),
                            rs.getDate("billed_at").toLocalDate().toString(),
                            rs.getTimestamp("created_at").toInstant().toEpochMilli()
                    ),
                    userId, Date.valueOf(range[0]), Date.valueOf(range[1])
            );
        }
        return jdbcTemplate.query(
                "SELECT id, direction, category, amount, note, billed_at, created_at FROM bill" +
                " WHERE user_id = ? ORDER BY billed_at DESC, id DESC",
                (rs, rowNum) -> new BillItem(
                        rs.getLong("id"),
                        rs.getString("direction"),
                        rs.getString("category"),
                        rs.getBigDecimal("amount"),
                        rs.getString("note"),
                        rs.getDate("billed_at").toLocalDate().toString(),
                        rs.getTimestamp("created_at").toInstant().toEpochMilli()
                ),
                userId
        );
    }

    public BillItem update(String userId, Long id, CreateBillRequest req) {
        LocalDate billedAt = parseDate(req.billedAt());
        int affected = jdbcTemplate.update(
                "UPDATE bill SET direction = ?, category = ?, amount = ?, note = ?, billed_at = ?," +
                " updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
                req.direction(), req.category(), req.amount(), req.note(),
                Date.valueOf(billedAt), id, userId
        );
        if (affected == 0) {
            throw new BusinessException("BILL_NOT_FOUND", "bill not found or access denied");
        }
        return new BillItem(id, req.direction(), req.category(), req.amount(), req.note(),
                req.billedAt(), System.currentTimeMillis());
    }

    public void delete(String userId, Long id) {
        int affected = jdbcTemplate.update(
                "DELETE FROM bill WHERE id = ? AND user_id = ?", id, userId
        );
        if (affected == 0) {
            throw new BusinessException("BILL_NOT_FOUND", "bill not found or access denied");
        }
    }

    public BillStatsDto stats(String userId, String month) {
        Object[] baseArgs;
        String whereClause;

        if (month != null && !month.isBlank()) {
            var range = monthRange(month);
            whereClause = " WHERE user_id = ? AND billed_at >= ? AND billed_at < ?";
            baseArgs = new Object[]{userId, Date.valueOf(range[0]), Date.valueOf(range[1].plusDays(1))};
        } else {
            whereClause = " WHERE user_id = ?";
            baseArgs = new Object[]{userId};
        }

        // Aggregate income and expense totals
        List<Map<String, Object>> dirRows = jdbcTemplate.queryForList(
                "SELECT direction, SUM(amount) AS total, COUNT(*) AS cnt FROM bill" +
                whereClause + " GROUP BY direction",
                baseArgs
        );

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int count = 0;
        for (Map<String, Object> row : dirRows) {
            String dir = (String) row.get("direction");
            BigDecimal total = new BigDecimal(row.get("total").toString());
            int cnt = ((Number) row.get("cnt")).intValue();
            count += cnt;
            if ("income".equals(dir)) {
                totalIncome = total;
            } else {
                totalExpense = total;
            }
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        BigDecimal savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                : null;

        // Per-category breakdown with direction
        List<CategoryStat> byCategory = jdbcTemplate.query(
                "SELECT direction, category, SUM(amount) AS amount, COUNT(*) AS cnt FROM bill" +
                whereClause + " GROUP BY direction, category ORDER BY amount DESC",
                (rs, rowNum) -> new CategoryStat(
                        rs.getString("direction"),
                        rs.getString("category"),
                        rs.getBigDecimal("amount"),
                        rs.getInt("cnt")
                ),
                baseArgs
        );

        return new BillStatsDto(totalIncome, totalExpense, netSavings, savingsRate, count, byCategory);
    }

    public List<MonthlyTrendItem> getMonthlyTrend(String userId, int months) {
        // H2 compatibility: use FORMATDATETIME instead of DATE_FORMAT.
        // At runtime on MySQL, DATE_FORMAT is used. We branch on dialect via a flag-free approach:
        // use a portable expression supported by both H2 (MySQL mode) and MySQL.
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(billed_at, '%Y-%m') AS month," +
                " direction, SUM(amount) AS total" +
                " FROM bill" +
                " WHERE user_id = ? AND billed_at >= DATE_SUB(CURDATE(), INTERVAL ? MONTH)" +
                " GROUP BY month, direction" +
                " ORDER BY month ASC",
                userId, months
        );

        // Merge rows by month, filling missing directions with ZERO
        Map<String, BigDecimal[]> byMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String month = (String) row.get("month");
            String dir = (String) row.get("direction");
            BigDecimal total = new BigDecimal(row.get("total").toString());
            // [0]=income, [1]=expense
            byMonth.computeIfAbsent(month, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if ("income".equals(dir)) {
                byMonth.get(month)[0] = total;
            } else {
                byMonth.get(month)[1] = total;
            }
        }

        List<MonthlyTrendItem> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : byMonth.entrySet()) {
            BigDecimal inc = entry.getValue()[0];
            BigDecimal exp = entry.getValue()[1];
            result.add(new MonthlyTrendItem(entry.getKey(), inc, exp, inc.subtract(exp)));
        }
        return result;
    }

    public SecurityReportDto getSecurityReport(String userId) {
        List<MonthlyTrendItem> trend = getMonthlyTrend(userId, 6);

        int dataMths = trend.size();
        int incomeMths = (int) trend.stream()
                .filter(m -> m.totalIncome().compareTo(BigDecimal.ZERO) > 0)
                .count();

        boolean hasEnoughData = incomeMths >= 2;

        BigDecimal sumIncome = trend.stream()
                .map(MonthlyTrendItem::totalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumExpense = trend.stream()
                .map(MonthlyTrendItem::totalExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumNet = sumIncome.subtract(sumExpense);

        int incomeDivisor = Math.max(incomeMths, 1);
        int dataDivisor = Math.max(dataMths, 1);

        BigDecimal avgMonthlyIncome = sumIncome.divide(BigDecimal.valueOf(incomeDivisor), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense = sumExpense.divide(BigDecimal.valueOf(dataDivisor), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlySavings = avgMonthlyIncome.subtract(avgMonthlyExpense);

        BigDecimal savingsRate = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? avgMonthlySavings.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP)
                : null;

        BigDecimal emergencyFundMonths = avgMonthlyExpense.compareTo(BigDecimal.ZERO) > 0
                ? sumNet.divide(avgMonthlyExpense, 2, RoundingMode.HALF_UP)
                : sumNet.setScale(2, RoundingMode.HALF_UP);

        // Status thresholds
        int savingsRateStatus;
        if (savingsRate == null || savingsRate.compareTo(new BigDecimal("0.1")) < 0) {
            savingsRateStatus = 0; // red
        } else if (savingsRate.compareTo(new BigDecimal("0.2")) < 0) {
            savingsRateStatus = 1; // yellow
        } else {
            savingsRateStatus = 2; // green
        }

        int emergencyFundStatus;
        if (emergencyFundMonths.compareTo(new BigDecimal("3")) < 0) {
            emergencyFundStatus = 0;
        } else if (emergencyFundMonths.compareTo(new BigDecimal("6")) < 0) {
            emergencyFundStatus = 1;
        } else {
            emergencyFundStatus = 2;
        }

        // Health score
        int score = 0;

        // Savings rate component (max 40)
        if (savingsRate != null) {
            if (savingsRate.compareTo(new BigDecimal("0.3")) >= 0) {
                score += 40;
            } else if (savingsRate.compareTo(new BigDecimal("0.2")) >= 0) {
                score += 30;
            } else if (savingsRate.compareTo(new BigDecimal("0.1")) >= 0) {
                score += 15;
            }
        }

        // Emergency fund component (max 30)
        if (emergencyFundMonths.compareTo(new BigDecimal("6")) >= 0) {
            score += 30;
        } else if (emergencyFundMonths.compareTo(new BigDecimal("3")) >= 0) {
            score += 20;
        } else if (emergencyFundMonths.compareTo(BigDecimal.ONE) >= 0) {
            score += 10;
        }

        // Income data completeness (max 20)
        if (incomeMths >= 3) {
            score += 20;
        } else if (incomeMths >= 1) {
            score += 10;
        }

        // Data completeness (max 10)
        if (dataMths >= 4) {
            score += 10;
        }

        String healthLevel;
        if (score >= 80) {
            healthLevel = "优秀";
        } else if (score >= 60) {
            healthLevel = "健康";
        } else if (score >= 40) {
            healthLevel = "警戒";
        } else {
            healthLevel = "危险";
        }

        String period = "最近" + Math.min(dataMths, 6) + "个月";

        return new SecurityReportDto(
                hasEnoughData,
                period,
                avgMonthlyIncome,
                avgMonthlyExpense,
                avgMonthlySavings,
                savingsRate,
                savingsRateStatus,
                emergencyFundMonths,
                emergencyFundStatus,
                score,
                healthLevel
        );
    }

    public FamilyBillStats familyStats(String userId, String month) {
        // 1. Get current user's own stats
        BillStatsDto selfStats = stats(userId, month);
        String selfName = jdbcTemplate.query(
                "SELECT nickname FROM users WHERE user_id = ?",
                (rs, rowNum) -> rs.getString("nickname"),
                userId
        ).stream().findFirst().orElse("户主");
        if (selfName == null || selfName.isBlank()) {
            selfName = "户主";
        }

        MemberStats selfMember = new MemberStats(
                "self",
                selfName,
                "本人",
                selfStats.totalIncome(),
                selfStats.totalExpense(),
                selfStats.netSavings()
        );

        List<MemberStats> allMembers = new ArrayList<>();
        allMembers.add(selfMember);

        // 2. Get family member phones
        List<Map<String, Object>> memberRows = jdbcTemplate.queryForList(
                "SELECT member_id, member_name, member_phone, relation_to_user" +
                " FROM family_members WHERE user_id = ?",
                userId
        );

        if (!memberRows.isEmpty()) {
            List<String> phones = memberRows.stream()
                    .map(r -> (String) r.get("member_phone"))
                    .toList();

            // Build IN clause placeholders
            String placeholders = phones.stream().map(p -> "?").collect(java.util.stream.Collectors.joining(","));
            List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                    "SELECT user_id, phone FROM users WHERE phone IN (" + placeholders + ")",
                    phones.toArray()
            );

            // Map phone -> userId
            Map<String, String> phoneToUserId = new java.util.HashMap<>();
            for (Map<String, Object> row : userRows) {
                phoneToUserId.put((String) row.get("phone"), (String) row.get("user_id"));
            }

            // 3. Get stats for each member who has an account
            for (Map<String, Object> memberRow : memberRows) {
                String memberId = (String) memberRow.get("member_id");
                String memberName = (String) memberRow.get("member_name");
                String memberPhone = (String) memberRow.get("member_phone");
                String relation = (String) memberRow.get("relation_to_user");
                String memberUserId = phoneToUserId.get(memberPhone);

                if (memberUserId != null) {
                    BillStatsDto memberStats = stats(memberUserId, month);
                    allMembers.add(new MemberStats(
                            memberId, memberName, relation,
                            memberStats.totalIncome(), memberStats.totalExpense(), memberStats.netSavings()
                    ));
                } else {
                    // Member has no account: contribute zero stats
                    allMembers.add(new MemberStats(
                            memberId, memberName, relation,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                    ));
                }
            }
        }

        // 4. Aggregate totals
        BigDecimal totalIncome = allMembers.stream()
                .map(MemberStats::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = allMembers.stream()
                .map(MemberStats::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        BigDecimal savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                : null;

        return new FamilyBillStats(allMembers, totalIncome, totalExpense, netSavings, savingsRate);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new BusinessException("BILL_INVALID_DATE", "invalid date format, expected yyyy-MM-dd");
        }
    }

    private LocalDate[] monthRange(String month) {
        try {
            var ym = YearMonth.parse(month);
            return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
        } catch (DateTimeParseException e) {
            throw new BusinessException("BILL_INVALID_MONTH", "invalid month format, expected yyyy-MM");
        }
    }
}
