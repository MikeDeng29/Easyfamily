package com.easyfamily.finance.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.finance.dto.FinanceDtos.GrantRequest;
import com.easyfamily.finance.dto.FinanceDtos.MyRoleResponse;
import com.easyfamily.finance.dto.FinanceDtos.PermissionListResponse;
import com.easyfamily.finance.service.FinancePermissionService;
import com.easyfamily.finance.service.FinanceRole;
import com.easyfamily.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
public class FinancePermissionController {

    private final FinancePermissionService financePermissionService;
    private final JdbcTemplate jdbcTemplate;

    public FinancePermissionController(FinancePermissionService financePermissionService,
                                       JdbcTemplate jdbcTemplate) {
        this.financePermissionService = financePermissionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * GET /api/v1/finance/my-role
     *
     * Returns the caller's role in the family finance system plus, when they
     * are a viewer, the head's display name.
     */
    @GetMapping("/my-role")
    public ApiResponse<MyRoleResponse> myRole() {
        var user = AuthContext.currentUser();
        FinanceRole role = financePermissionService.resolveRole(user.userId(), user.phone());

        String headUserId = role.headUserId();
        String headName = null;
        if (role.isViewer() && headUserId != null) {
            headName = resolveHeadName(headUserId);
        }

        return ApiResponse.ok(new MyRoleResponse(role.role(), headUserId, headName));
    }

    /**
     * GET /api/v1/finance/permissions  (head only)
     *
     * Lists phone numbers that are authorised to view the caller's financial data.
     * Phone numbers are masked (e.g. 138****8000).
     */
    @GetMapping("/permissions")
    public ApiResponse<PermissionListResponse> listPermissions() {
        var user = AuthContext.currentUser();
        requireHead(user.userId(), user.phone());
        List<String> viewers = financePermissionService.listViewers(user.userId());
        return ApiResponse.ok(new PermissionListResponse(viewers));
    }

    /**
     * POST /api/v1/finance/permissions  (head only)
     *
     * Grants a phone number read access to the caller's financial data.
     * The phone number does not need to have an existing account.
     */
    @PostMapping("/permissions")
    public ApiResponse<Void> grantPermission(@Valid @RequestBody GrantRequest req) {
        var user = AuthContext.currentUser();
        requireHead(user.userId(), user.phone());
        financePermissionService.grantViewer(user.userId(), req.phone());
        return ApiResponse.ok(null);
    }

    /**
     * DELETE /api/v1/finance/permissions/{phone}  (head only)
     *
     * Revokes a previously granted phone number's read access.
     */
    @DeleteMapping("/permissions/{phone}")
    public ApiResponse<Void> revokePermission(@PathVariable String phone) {
        var user = AuthContext.currentUser();
        requireHead(user.userId(), user.phone());
        financePermissionService.revokeViewer(user.userId(), phone);
        return ApiResponse.ok(null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void requireHead(String userId, String phone) {
        FinanceRole role = financePermissionService.resolveRole(userId, phone);
        if (!role.isHead()) {
            throw new BusinessException("FINANCE_ACCESS_DENIED",
                    "only household head can manage permissions");
        }
    }

    /**
     * Resolves a display name for a head user: prefers their nickname, falls
     * back to the last four digits of their phone number.
     */
    private String resolveHeadName(String headUserId) {
        List<String[]> rows = jdbcTemplate.query(
                "SELECT nickname, phone FROM users WHERE user_id = ? LIMIT 1",
                (rs, n) -> new String[]{rs.getString("nickname"), rs.getString("phone")},
                headUserId
        );
        if (rows.isEmpty()) {
            return null;
        }
        String nickname = rows.get(0)[0];
        String phone = rows.get(0)[1];
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (phone != null && phone.length() >= 4) {
            return phone.substring(phone.length() - 4);
        }
        return null;
    }
}
