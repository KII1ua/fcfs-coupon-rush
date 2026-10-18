package com.coupon.rush.coupon.issued.adapter.in.web;

import jakarta.validation.constraints.NotNull;

public record IssueCouponRequest(@NotNull Long roundId, @NotNull Long usersId) {
}
