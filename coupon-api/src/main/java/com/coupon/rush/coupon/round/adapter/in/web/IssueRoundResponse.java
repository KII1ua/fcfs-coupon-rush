package com.coupon.rush.coupon.round.adapter.in.web;

import com.coupon.rush.coupon.round.domain.IssueRound;

import java.time.Instant;
import java.time.LocalDateTime;

public record IssueRoundResponse(
        Long id,
        Long policyId,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        int totalQuantity,
        Instant createdAt,
        Instant updatedAt
) {
    public static IssueRoundResponse from(IssueRound round) {
        return new IssueRoundResponse(
                round.getId(),
                round.getPolicyId(),
                round.getOpenedAt(),
                round.getClosedAt(),
                round.getTotalQuantity(),
                round.getCreatedAt(),
                round.getUpdatedAt()
        );
    }
}
