package com.easyfamily.family.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberCreateRequest;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberItem;
import com.easyfamily.family.dto.FamilyDtos.FamilyMemberUpdateRequest;
import com.easyfamily.family.service.FamilyMemberService;
import com.easyfamily.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/family/members")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    public FamilyMemberController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @GetMapping
    public ApiResponse<List<FamilyMemberItem>> listMembers() {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(familyMemberService.listMembers(current.userId()));
    }

    @GetMapping("/{memberId}")
    public ApiResponse<FamilyMemberItem> getMember(@PathVariable String memberId) {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(familyMemberService.getMember(current.userId(), memberId));
    }

    @PostMapping
    public ApiResponse<FamilyMemberItem> createMember(@Valid @RequestBody FamilyMemberCreateRequest request) {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(familyMemberService.createMember(current.userId(), request));
    }

    @PutMapping("/{memberId}")
    public ApiResponse<FamilyMemberItem> updateMember(
            @PathVariable String memberId,
            @Valid @RequestBody FamilyMemberUpdateRequest request
    ) {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(familyMemberService.updateMember(current.userId(), memberId, request));
    }

    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> deleteMember(@PathVariable String memberId) {
        var current = AuthContext.currentUser();
        familyMemberService.deleteMember(current.userId(), memberId);
        return ApiResponse.ok(null);
    }
}
