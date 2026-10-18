package com.coupon.rush.coupon.issued.adapter.out.persistence;

import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
    List<IssuedCoupon> findAllByUsersId(Long usersId);
    boolean existsByRoundIdAndUsersId(Long roundId, Long usersId);
    boolean existsByRoundId(Long roundId);
    long countByRoundId(Long roundId);
}
