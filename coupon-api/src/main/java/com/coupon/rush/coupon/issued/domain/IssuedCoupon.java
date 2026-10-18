package com.coupon.rush.coupon.issued.domain;

import com.coupon.rush._config.BaseTimeEntity;
import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon extends BaseTimeEntity {

    private Long roundId;

    private Long usersId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private IssuedCoupon(Long roundId, Long usersId) {
        this.roundId = roundId;
        this.usersId = usersId;
        this.status = Status.ISSUED;
    }

    public static IssuedCoupon issue(Long roundId, Long usersId) {
        return new IssuedCoupon(roundId, usersId);
    }

    public void use() {
        if (status != Status.ISSUED) {
            throw new BusinessException(ErrorCode.COUPON_NOT_USABLE);
        }
        this.status = Status.USED;
    }
}
