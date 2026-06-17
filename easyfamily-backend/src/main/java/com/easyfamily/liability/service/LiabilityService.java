package com.easyfamily.liability.service;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityCreateRequest;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityItem;
import com.easyfamily.liability.dto.LiabilityDtos.LiabilityListResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class LiabilityService {

    private static final Set<String> VALID_LIABILITY_TYPES =
            Set.of("mortgage", "car_loan", "credit_card", "personal_loan", "other");

    private final JdbcTemplate jdbcTemplate;

    public LiabilityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LiabilityListResponse list(String userId) {
        List<LiabilityItem> items = jdbcTemplate.query(
                "SELECT id, name, liability_type, balance, monthly_payment, interest_rate, note, created_at" +
                " FROM family_liability WHERE user_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> new LiabilityItem(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("liability_type"),
                        rs.getBigDecimal("balance"),
                        rs.getBigDecimal("monthly_payment"),
                        rs.getBigDecimal("interest_rate"),
                        rs.getString("note"),
                        rs.getTimestamp("created_at").toLocalDateTime().toLocalDate().toString()
                ),
                userId
        );
        BigDecimal totalBalance = items.stream()
                .map(LiabilityItem::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMonthlyPayment = items.stream()
                .map(i -> i.monthlyPayment() != null ? i.monthlyPayment() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LiabilityListResponse(items, totalBalance, totalMonthlyPayment);
    }

    public LiabilityItem create(String userId, LiabilityCreateRequest req) {
        validateLiabilityType(req.liabilityType());
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO family_liability" +
                    "(user_id, name, liability_type, balance, monthly_payment, interest_rate, note, created_at, updated_at)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, userId);
            ps.setString(2, req.name());
            ps.setString(3, req.liabilityType());
            ps.setBigDecimal(4, req.balance());
            ps.setBigDecimal(5, req.monthlyPayment());
            ps.setBigDecimal(6, req.interestRate());
            ps.setString(7, req.note());
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        String now = LocalDate.now().toString();
        return new LiabilityItem(id, req.name(), req.liabilityType(), req.balance(),
                req.monthlyPayment(), req.interestRate(), req.note(), now);
    }

    public LiabilityItem update(String userId, Long id, LiabilityCreateRequest req) {
        validateLiabilityType(req.liabilityType());
        int affected = jdbcTemplate.update(
                "UPDATE family_liability SET name = ?, liability_type = ?, balance = ?," +
                " monthly_payment = ?, interest_rate = ?, note = ?," +
                " updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
                req.name(), req.liabilityType(), req.balance(),
                req.monthlyPayment(), req.interestRate(), req.note(), id, userId
        );
        if (affected == 0) {
            throw new BusinessException("LIABILITY_NOT_FOUND", "liability not found or access denied");
        }
        String now = LocalDate.now().toString();
        return new LiabilityItem(id, req.name(), req.liabilityType(), req.balance(),
                req.monthlyPayment(), req.interestRate(), req.note(), now);
    }

    public void delete(String userId, Long id) {
        int affected = jdbcTemplate.update(
                "DELETE FROM family_liability WHERE id = ? AND user_id = ?", id, userId
        );
        if (affected == 0) {
            throw new BusinessException("LIABILITY_NOT_FOUND", "liability not found or access denied");
        }
    }

    private void validateLiabilityType(String liabilityType) {
        if (!VALID_LIABILITY_TYPES.contains(liabilityType)) {
            throw new BusinessException("LIABILITY_INVALID_TYPE",
                    "liability_type must be one of: mortgage, car_loan, credit_card, personal_loan, other");
        }
    }
}
