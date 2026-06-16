package com.easyfamily.user.controller;

import com.easyfamily.common.api.ApiResponse;
import com.easyfamily.security.AuthContext;
import com.easyfamily.user.dto.UserProfileDtos.UpdateButlerRequest;
import com.easyfamily.user.dto.UserProfileDtos.UpdateNicknameRequest;
import com.easyfamily.user.dto.UserProfileDtos.UserProfile;
import com.easyfamily.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile() {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(userProfileService.getProfile(current.userId(), current.phone()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfile> updateProfile(@Valid @RequestBody UpdateNicknameRequest request) {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(userProfileService.updateNickname(current.userId(), current.phone(), request.nickname().trim()));
    }

    @PutMapping("/butler")
    public ApiResponse<UserProfile> updateButler(@RequestBody UpdateButlerRequest request) {
        var current = AuthContext.currentUser();
        return ApiResponse.ok(userProfileService.updateButler(current.userId(), current.phone(), request));
    }
}
