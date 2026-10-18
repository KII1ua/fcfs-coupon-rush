package com.coupon.rush.coupon.policy.application.port.out;

import com.coupon.rush.coupon.policy.domain.CouponPolicy;

import java.util.List;
import java.util.Optional;

public interface CouponPolicyRepository {
    CouponPolicy save(CouponPolicy policy);
    Optional<CouponPolicy> findById(Long id);
    List<CouponPolicy> findAll();
    boolean existsById(Long id);
    void delete(CouponPolicy policy);
}
