package com.coupon.rush.coupon.round.domain;

import com.coupon.rush._config.BaseTimeEntity;
import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueRound extends BaseTimeEntity {

    private Long policyId;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private int totalQuantity;

    private IssueRound(Long policyId, LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        this.policyId = policyId;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.totalQuantity = totalQuantity;
    }

    public static IssueRound create(Long policyId, LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        validate(openedAt, closedAt, totalQuantity);
        return new IssueRound(policyId, openedAt, closedAt, totalQuantity);
    }

    public void update(LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        validate(openedAt, closedAt, totalQuantity);
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.totalQuantity = totalQuantity;
    }

    public boolean isBeforeOpen(LocalDateTime now) {
        return now.isBefore(openedAt);
    }

    public boolean isClosed(LocalDateTime now) {
        return !now.isBefore(closedAt);
    }

    public boolean isSoldOut(long issuedCount) {
        return issuedCount >= totalQuantity;
    }

    private static void validate(LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        if (openedAt == null || closedAt == null || !openedAt.isBefore(closedAt) || totalQuantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ROUND);
        }
    }
}
