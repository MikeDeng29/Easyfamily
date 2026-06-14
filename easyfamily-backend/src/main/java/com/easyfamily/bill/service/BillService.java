package com.easyfamily.bill.service;

import com.easyfamily.bill.dto.BillDtos.BillItem;
import com.easyfamily.bill.dto.BillDtos.BillStatsDto;
import com.easyfamily.bill.dto.BillDtos.CategoryStat;
import com.easyfamily.bill.dto.BillDtos.CreateBillRequest;
import com.easyfamily.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
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
                    "INSERT INTO bill(user_id, category, amount, note, billed_at, created_at, updated_at)" +
                    " VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, req.category());
            ps.setBigDecimal(3, req.amount());
            ps.setString(4, req.note());
            ps.setDate(5, Date.valueOf(billedAt));
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new BillItem(id, req.category(), req.amount(), req.note(), req.billedAt(),
                System.currentTimeMillis());
    }

    public List<BillItem> list(String userId, String month) {
        if (month != null && !month.isBlank()) {
            var range = monthRange(month);
            return jdbcTemplate.query(
                    "SELECT id, category, amount, note, billed_at, created_at FROM bill" +
                    " WHERE user_id = ? AND billed_at BETWEEN ? AND ? ORDER BY billed_at DESC, id DESC",
                    (rs, rowNum) -> new BillItem(
                            rs.getLong("id"),
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
                "SELECT id, category, amount, note, billed_at, created_at FROM bill" +
                " WHERE user_id = ? ORDER BY billed_at DESC, id DESC",
                (rs, rowNum) -> new BillItem(
                        rs.getLong("id"),
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
                "UPDATE bill SET category = ?, amount = ?, note = ?, billed_at = ?, updated_at = CURRENT_TIMESTAMP" +
                " WHERE id = ? AND user_id = ?",
                req.category(), req.amount(), req.note(), Date.valueOf(billedAt), id, userId
        );
        if (affected == 0) {
            throw new BusinessException("BILL_NOT_FOUND", "bill not found or access denied");
        }
        return new BillItem(id, req.category(), req.amount(), req.note(), req.billedAt(),
                System.currentTimeMillis());
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
        List<CategoryStat> byCategory;
        BigDecimal totalAmount;
        int count;

        if (month != null && !month.isBlank()) {
            var range = monthRange(month);
            byCategory = jdbcTemplate.query(
                    "SELECT category, SUM(amount) AS total, COUNT(*) AS cnt FROM bill" +
                    " WHERE user_id = ? AND billed_at BETWEEN ? AND ? GROUP BY category ORDER BY total DESC",
                    (rs, rowNum) -> new CategoryStat(
                            rs.getString("category"),
                            rs.getBigDecimal("total"),
                            rs.getInt("cnt")
                    ),
                    userId, Date.valueOf(range[0]), Date.valueOf(range[1])
            );
            var summary = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(amount), 0) AS total, COUNT(*) AS cnt FROM bill" +
                    " WHERE user_id = ? AND billed_at BETWEEN ? AND ?",
                    userId, Date.valueOf(range[0]), Date.valueOf(range[1])
            );
            totalAmount = new BigDecimal(summary.get("total").toString());
            count = ((Number) summary.get("cnt")).intValue();
        } else {
            byCategory = jdbcTemplate.query(
                    "SELECT category, SUM(amount) AS total, COUNT(*) AS cnt FROM bill" +
                    " WHERE user_id = ? GROUP BY category ORDER BY total DESC",
                    (rs, rowNum) -> new CategoryStat(
                            rs.getString("category"),
                            rs.getBigDecimal("total"),
                            rs.getInt("cnt")
                    ),
                    userId
            );
            var summary = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(amount), 0) AS total, COUNT(*) AS cnt FROM bill WHERE user_id = ?",
                    userId
            );
            totalAmount = new BigDecimal(summary.get("total").toString());
            count = ((Number) summary.get("cnt")).intValue();
        }
        return new BillStatsDto(totalAmount, count, byCategory);
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
