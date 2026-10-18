package com.coupon.rush.user.adapter.in.web;

import com.coupon.rush.user.domain.User;

import java.time.Instant;

public record UserResponse(Long id, String userId, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUserId(), user.getCreatedAt());
    }
}
