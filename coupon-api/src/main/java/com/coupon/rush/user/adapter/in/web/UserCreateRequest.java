package com.coupon.rush.user.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String userId,
        @NotBlank @Size(max = 255) String password
) {
}
