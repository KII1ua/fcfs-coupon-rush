package com.coupon.rush.coupon.policy.domain;

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
public class CouponPolicy extends BaseTimeEntity {

    private String name;

    private int discountValue;

    private int maxDiscountAmount;

    private LocalDateTime validUntil;

    private CouponPolicy(String name, int discountValue, int maxDiscountAmount, LocalDateTime validUntil) {
        this.name = name;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validUntil = validUntil;
    }

    public static CouponPolicy create(String name, int discountValue, int maxDiscountAmount, LocalDateTime validUntil) {
        validate(discountValue, maxDiscountAmount, validUntil);
        return new CouponPolicy(name, discountValue, maxDiscountAmount, validUntil);
    }

    public void update(String name, int discountValue, int maxDiscountAmount, LocalDateTime validUntil) {
        validate(discountValue, maxDiscountAmount, validUntil);
        this.name = name;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validUntil = validUntil;
    }

    private static void validate(int discountValue, int maxDiscountAmount, LocalDateTime validUntil) {
        if (discountValue <= 0 || maxDiscountAmount <= 0 || validUntil == null) {
            throw new BusinessException(ErrorCode.INVALID_POLICY);
        }
    }
}
