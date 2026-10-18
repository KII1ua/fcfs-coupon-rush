package com.coupon.rush.coupon.round.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record IssueRoundCreateRequest(
        @NotNull Long policyId,
        @NotNull LocalDateTime openedAt,
        @NotNull LocalDateTime closedAt,
        @NotNull @Positive Integer totalQuantity
) {
}
