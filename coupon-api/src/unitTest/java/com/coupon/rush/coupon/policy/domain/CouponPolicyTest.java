package com.coupon.rush.coupon.policy.domain;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyTest {

    private static final LocalDateTime VALID_UNTIL = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    @Test
    void 정상_값으로_생성된다() {
        CouponPolicy policy = CouponPolicy.create("신규 가입 쿠폰", 10, 5000, VALID_UNTIL);

        assertThat(policy.getName()).isEqualTo("신규 가입 쿠폰");
        assertThat(policy.getDiscountValue()).isEqualTo(10);
        assertThat(policy.getMaxDiscountAmount()).isEqualTo(5000);
        assertThat(policy.getValidUntil()).isEqualTo(VALID_UNTIL);
    }

    @Test
    void 할인값이_0_이하면_생성_실패() {
        assertInvalid(() -> CouponPolicy.create("p", 0, 5000, VALID_UNTIL));
        assertInvalid(() -> CouponPolicy.create("p", -1, 5000, VALID_UNTIL));
    }

    @Test
    void 최대할인금액이_0_이하면_생성_실패() {
        assertInvalid(() -> CouponPolicy.create("p", 10, 0, VALID_UNTIL));
        assertInvalid(() -> CouponPolicy.create("p", 10, -1, VALID_UNTIL));
    }

    @Test
    void 유효기간이_없으면_생성_실패() {
        assertInvalid(() -> CouponPolicy.create("p", 10, 5000, null));
    }

    @Test
    void 정상_값으로_수정된다() {
        CouponPolicy policy = CouponPolicy.create("p", 10, 5000, VALID_UNTIL);
        LocalDateTime newValidUntil = VALID_UNTIL.plusDays(30);

        policy.update("수정됨", 20, 10000, newValidUntil);

        assertThat(policy.getName()).isEqualTo("수정됨");
        assertThat(policy.getDiscountValue()).isEqualTo(20);
        assertThat(policy.getMaxDiscountAmount()).isEqualTo(10000);
        assertThat(policy.getValidUntil()).isEqualTo(newValidUntil);
    }

    @Test
    void 잘못된_값으로_수정하면_실패하고_기존_값이_유지된다() {
        CouponPolicy policy = CouponPolicy.create("p", 10, 5000, VALID_UNTIL);

        assertInvalid(() -> policy.update("x", 0, 5000, VALID_UNTIL));
        assertInvalid(() -> policy.update("x", 10, 0, VALID_UNTIL));
        assertInvalid(() -> policy.update("x", 10, 5000, null));

        assertThat(policy.getName()).isEqualTo("p");
        assertThat(policy.getDiscountValue()).isEqualTo(10);
        assertThat(policy.getMaxDiscountAmount()).isEqualTo(5000);
        assertThat(policy.getValidUntil()).isEqualTo(VALID_UNTIL);
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_POLICY);
    }
}
