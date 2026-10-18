package com.coupon.rush.consumer.issued.adapter.out.persistence;

import com.coupon.rush.consumer.issued.domain.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
    boolean existsByRoundIdAndUsersId(Long roundId, Long usersId);
}
