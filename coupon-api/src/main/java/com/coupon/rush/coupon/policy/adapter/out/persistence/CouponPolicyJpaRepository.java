package com.coupon.rush.coupon.policy.adapter.out.persistence;

import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {
}
