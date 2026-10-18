package com.coupon.rush.consumer.issued.application.port.out;

import com.coupon.rush.consumer.issued.domain.IssuedCoupon;

public interface IssuedCouponRepository {
    boolean existsByRoundIdAndUsersId(Long roundId, Long usersId);

    // 같은 회차·같은 유저가 이미 있으면 DuplicateIssueException 을 던진다
    IssuedCoupon save(IssuedCoupon issuedCoupon);
}
