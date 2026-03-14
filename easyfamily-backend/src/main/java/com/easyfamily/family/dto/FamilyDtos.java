package com.easyfamily.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class FamilyDtos {

    private FamilyDtos() {
    }

    public record FamilyMemberItem(
            String memberId,
            String name,
            String phone,
            String relation
    ) {
    }

    public record FamilyMemberCreateRequest(
            @NotBlank String name,
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank String relation
    ) {
    }

    public record FamilyMemberUpdateRequest(
            @NotBlank String name,
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank String relation
    ) {
    }
}
