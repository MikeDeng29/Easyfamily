package com.easyfamily.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserProfileDtos {

    private UserProfileDtos() {
    }

    public record UserProfile(
            String userId,
            String phone,
            String nickname,
            String email,
            String butlerName,
            Integer butlerAvatarId,
            String butlerPersona
    ) {
    }

    public record UpdateNicknameRequest(
            @NotBlank
            @Size(min = 1, max = 20, message = "nickname length must be between 1 and 20")
            String nickname
    ) {
    }

    /**
     * Partial update request for AI butler identity customization. All fields are
     * optional -- only non-null fields are validated and persisted by
     * {@code UserProfileService#updateButler}.
     */
    public record UpdateEmailRequest(
            @Email
            @Size(max = 200)
            String email
    ) {
    }

    public record UpdateNameRequest(
            @Size(max = 50)
            String name
    ) {
    }

    public record UpdateButlerRequest(
            String butlerName,
            Integer butlerAvatarId,
            String butlerPersona
    ) {
    }
}
