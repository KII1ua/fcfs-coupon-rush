package com.coupon.rush.coupon.issued.domain;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuedCouponTest {

    @Test
    void issue_발급_상태로_생성된다() {
        IssuedCoupon coupon = IssuedCoupon.issue(1L, 2L);

        assertThat(coupon.getRoundId()).isEqualTo(1L);
        assertThat(coupon.getUsersId()).isEqualTo(2L);
        assertThat(coupon.getStatus()).isEqualTo(Status.ISSUED);
    }

    @Test
    void use_발급_상태면_사용_상태가_된다() {
        IssuedCoupon coupon = IssuedCoupon.issue(1L, 2L);

        coupon.use();

        assertThat(coupon.getStatus()).isEqualTo(Status.USED);
    }

    @Test
    void use_이미_사용한_쿠폰이면_예외() {
        IssuedCoupon coupon = IssuedCoupon.issue(1L, 2L);
        coupon.use();

        assertThatThrownBy(coupon::use)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_NOT_USABLE);
    }
}
