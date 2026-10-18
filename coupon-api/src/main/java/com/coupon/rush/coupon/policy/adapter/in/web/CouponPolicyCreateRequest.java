package com.coupon.rush.coupon.policy.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CouponPolicyCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Integer discountValue,
        @NotNull @Positive Integer maxDiscountAmount,
        @NotNull LocalDateTime validUntil
) {
}
