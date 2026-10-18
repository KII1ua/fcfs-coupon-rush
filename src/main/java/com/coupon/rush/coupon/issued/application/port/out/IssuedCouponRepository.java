package com.coupon.rush.coupon.issued.application.port.out;

import com.coupon.rush.coupon.issued.domain.IssuedCoupon;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponRepository {
    IssuedCoupon save(IssuedCoupon issuedCoupon);
    Optional<IssuedCoupon> findById(Long id);
    List<IssuedCoupon> findAllByUsersId(Long usersId);
    boolean existsByRoundIdAndUsersId(Long roundId, Long usersId);
    boolean existsByRoundId(Long roundId);
    long countByRoundId(Long roundId);
}
