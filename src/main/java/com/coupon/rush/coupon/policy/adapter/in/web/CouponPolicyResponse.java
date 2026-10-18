package com.coupon.rush.coupon.policy.adapter.in.web;

import com.coupon.rush.coupon.policy.domain.CouponPolicy;

import java.time.Instant;
import java.time.LocalDateTime;

public record CouponPolicyResponse(
        Long id,
        String name,
        int discountValue,
        int maxDiscountAmount,
        LocalDateTime validUntil,
        Instant createdAt,
        Instant updatedAt
) {
    public static CouponPolicyResponse from(CouponPolicy policy) {
        return new CouponPolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getDiscountValue(),
                policy.getMaxDiscountAmount(),
                policy.getValidUntil(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
