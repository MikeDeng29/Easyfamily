package com.easyfamily.query.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.query.dto.QueryDtos.RealNameVerifyRequest;
import com.easyfamily.query.dto.QueryDtos.RealNameVerifyResponse;
import com.easyfamily.query.service.QueryFacade;
import com.easyfamily.security.AuthContext;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryFacade queryFacade;

    public QueryController(QueryFacade queryFacade) {
        this.queryFacade = queryFacade;
    }

    @PostMapping("/real-name")
    public ApiResponse<RealNameVerifyResponse> verifyRealName(
            @Valid @RequestBody RealNameVerifyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        var current = AuthContext.currentUser();
        String ip = resolveClientIp(httpServletRequest);
        return ApiResponse.ok(queryFacade.verifyRealName(current.userId(), current.phone(), ip, request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
