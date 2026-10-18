package com.coupon.rush.coupon.issued.adapter.in.web;

import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import com.coupon.rush.coupon.issued.domain.Status;

import java.time.Instant;

public record IssuedCouponResponse(
        Long id,
        Long roundId,
        Long usersId,
        Status status,
        Instant createdAt,
        Instant updatedAt
) {
    public static IssuedCouponResponse from(IssuedCoupon coupon) {
        return new IssuedCouponResponse(
                coupon.getId(),
                coupon.getRoundId(),
                coupon.getUsersId(),
                coupon.getStatus(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}
