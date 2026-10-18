package com.coupon.rush.coupon.policy.adapter.out.persistence;

import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CouponPolicyPersistenceAdapter implements CouponPolicyRepository {
    private final CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Override
    public CouponPolicy save(CouponPolicy policy) {
        return couponPolicyJpaRepository.save(policy);
    }

    @Override
    public Optional<CouponPolicy> findById(Long id) {
        return couponPolicyJpaRepository.findById(id);
    }

    @Override
    public List<CouponPolicy> findAll() {
        return couponPolicyJpaRepository.findAll();
    }

    @Override
    public boolean existsById(Long id) {
        return couponPolicyJpaRepository.existsById(id);
    }

    @Override
    public void delete(CouponPolicy policy) {
        couponPolicyJpaRepository.delete(policy);
    }
}
